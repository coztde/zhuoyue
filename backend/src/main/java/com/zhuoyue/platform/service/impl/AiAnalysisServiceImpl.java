package com.zhuoyue.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhuoyue.platform.client.CommitInfo;
import com.zhuoyue.platform.client.GitHubApiClient;
import com.zhuoyue.platform.client.GiteeApiClient;
import com.zhuoyue.platform.common.exception.BizException;
import com.zhuoyue.platform.entity.MissionStage;
import com.zhuoyue.platform.entity.StudentProfile;
import com.zhuoyue.platform.entity.StudentRepo;
import com.zhuoyue.platform.entity.StudentStageProgress;
import com.zhuoyue.platform.mapper.MissionStageMapper;
import com.zhuoyue.platform.mapper.StudentProfileMapper;
import com.zhuoyue.platform.mapper.StudentRepoMapper;
import com.zhuoyue.platform.mapper.StudentStageProgressMapper;
import com.zhuoyue.platform.service.AiAnalysisService;
import com.zhuoyue.platform.vo.StageProgressVO;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI 分析 Service 实现。
 * 核心流程：拉取学生仓库 commit（message + diff）→ 构建 Prompt → 调用千问大模型 → 解析评分/评语 → 写回数据库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisServiceImpl implements AiAnalysisService {

    private final StudentProfileMapper studentProfileMapper;
    private final StudentRepoMapper studentRepoMapper;
    private final StudentStageProgressMapper progressMapper;
    private final MissionStageMapper missionStageMapper;
    private final GitHubApiClient gitHubApiClient;
    private final GiteeApiClient giteeApiClient;

    /** LangChain4j 自动注入的千问 ChatModel（通过 application.yml 配置）。 */
    private final ChatModel chatLanguageModel;

    /** 用于从 AI 响应中提取分数的正则，匹配「分数：85」或「85分」等格式。 */
    private static final Pattern SCORE_PATTERN = Pattern.compile("(?:分数[：:]\\s*|得分[：:]\\s*)(\\d{1,3})|^(\\d{1,3})(?:分|/100)", Pattern.MULTILINE);

    @Override
    public void analyzeStudent(Long studentId, Long repoId) {
        List<MissionStage> stages = missionStageMapper.selectList(null);
        for (MissionStage stage : stages) {
            try {
                analyzeStageProgress(studentId, stage.getId(), repoId);
            } catch (Exception e) {
                log.warn("批量分析跳过，studentId={}，stageId={}，原因：{}", studentId, stage.getId(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public StageProgressVO analyzeStageProgress(Long studentId, Long stageId, Long repoId) {
        StudentProfile student = studentProfileMapper.selectById(studentId);
        if (student == null) throw new BizException("NOT_FOUND", "学生不存在，id=" + studentId);

        MissionStage stage = missionStageMapper.selectById(stageId);
        if (stage == null) throw new BizException("NOT_FOUND", "阶段不存在，id=" + stageId);

        // 优先使用指定仓库，否则使用主仓库，最后 fallback 到所有激活仓库
        List<StudentRepo> repos;
        if (repoId != null) {
            StudentRepo repo = studentRepoMapper.selectById(repoId);
            if (repo == null) throw new BizException("NOT_FOUND", "仓库不存在，id=" + repoId);
            repos = List.of(repo);
        } else {
            List<StudentRepo> allRepos = studentRepoMapper.selectList(
                new LambdaQueryWrapper<StudentRepo>()
                    .eq(StudentRepo::getStudentId, studentId)
                    .eq(StudentRepo::getIsActive, true)
            );
            // 若有主仓库，只用主仓库分析；否则遍历所有激活仓库
            repos = allRepos.stream().anyMatch(r -> Boolean.TRUE.equals(r.getIsPrimary()))
                ? allRepos.stream().filter(r -> Boolean.TRUE.equals(r.getIsPrimary())).toList()
                : allRepos;
        }

        if (repos.isEmpty()) {
            throw new BizException("NO_REPO", "学生尚未配置任何仓库，请先添加仓库");
        }

        // 构建 commit 内容（遍历所有仓库，收集时间窗口内的 commit）
        String commitContent = buildCommitContent(student, repos, stage);

        // 任务描述：优先用 taskDescription，fallback 到 taskTitle
        String taskDesc = (stage.getTaskDescription() != null && !stage.getTaskDescription().isBlank())
            ? stage.getTaskDescription() : stage.getTaskTitle();

        // 构建发给大模型的 Prompt
        String prompt = buildPrompt(student.getRealName(), stage.getTitle(), taskDesc, commitContent);

        log.info("开始 AI 分析，学生={}，阶段={}，prompt长度={}", student.getRealName(), stage.getTitle(), prompt.length());

        // 调用千问大模型
        String aiResponse;
        try {
            aiResponse = chatLanguageModel.chat(prompt);
        } catch (Exception e) {
            log.error("AI 分析调用失败，studentId={}，stageId={}，原因：{}", studentId, stageId, e.getMessage());
            throw new BizException("AI_ERROR", "AI 分析调用失败：" + e.getMessage());
        }

        // 从响应中解析分数（0-100）
        short score = parseScore(aiResponse);

        // 写回或更新进度记录
        StudentStageProgress progress = progressMapper.selectOne(
            new LambdaQueryWrapper<StudentStageProgress>()
                .eq(StudentStageProgress::getStudentId, studentId)
                .eq(StudentStageProgress::getStageId, stageId)
        );

        if (progress == null) {
            // 尚无进度记录，先创建一条基础记录
            progress = new StudentStageProgress();
            progress.setStudentId(studentId);
            progress.setStageId(stageId);
            progress.setIsCompleted(false);
            progress.setCommitCount(0);
            progress.setSyncedAt(LocalDateTime.now());
            progressMapper.insert(progress);
        }

        // 更新 AI 分析结果
        progressMapper.update(null,
            new LambdaUpdateWrapper<StudentStageProgress>()
                .eq(StudentStageProgress::getId, progress.getId())
                .set(StudentStageProgress::getAiScore, score)
                .set(StudentStageProgress::getAiFeedback, aiResponse)
                .set(StudentStageProgress::getAiAnalyzedAt, LocalDateTime.now())
        );

        log.info("AI 分析完成，学生={}，阶段={}，得分={}", student.getRealName(), stage.getTitle(), score);

        // 重新查询并返回最新进度 VO
        return toVO(progressMapper.selectById(progress.getId()), stage, repos);
    }

    @Override
    public void analyzeAll() {
        // 查询所有 ACTIVE 学生
        List<StudentProfile> students = studentProfileMapper.selectList(
            new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getActiveStatus, "ACTIVE")
        );
        List<MissionStage> stages = missionStageMapper.selectList(null);

        log.info("[AI全量分析] 开始，共 {} 名学生，{} 个阶段", students.size(), stages.size());

        // 24 小时内已分析过的跳过，避免重复消耗 token
        LocalDateTime skipBefore = LocalDateTime.now().minusHours(24);

        for (StudentProfile student : students) {
            for (MissionStage stage : stages) {
                try {
                    StudentStageProgress existing = progressMapper.selectOne(
                        new LambdaQueryWrapper<StudentStageProgress>()
                            .eq(StudentStageProgress::getStudentId, student.getId())
                            .eq(StudentStageProgress::getStageId, stage.getId())
                    );
                    // 24 小时内已分析过则跳过
                    if (existing != null && existing.getAiAnalyzedAt() != null
                            && existing.getAiAnalyzedAt().isAfter(skipBefore)) {
                        continue;
                    }
                    analyzeStageProgress(student.getId(), stage.getId(), null);
                } catch (Exception e) {
                    log.warn("[AI全量分析] 跳过，studentId={}，stageId={}，原因：{}",
                        student.getId(), stage.getId(), e.getMessage());
                }
            }
        }
        log.info("[AI全量分析] 完成");
    }

    @Override
    public List<StageProgressVO> listProgressByStudent(Long studentId) {
        List<MissionStage> stages = missionStageMapper.selectList(null);
        List<StudentRepo> repos = studentRepoMapper.selectList(
            new LambdaQueryWrapper<StudentRepo>().eq(StudentRepo::getStudentId, studentId)
        );
        Map<Long, StudentRepo> repoMap = repos.stream()
            .collect(Collectors.toMap(StudentRepo::getId, r -> r, (a, b) -> a));

        return stages.stream().map(stage -> {
            StudentStageProgress progress = progressMapper.selectOne(
                new LambdaQueryWrapper<StudentStageProgress>()
                    .eq(StudentStageProgress::getStudentId, studentId)
                    .eq(StudentStageProgress::getStageId, stage.getId())
            );
            return toVO(progress, stage, repos);
        }).toList();
    }

    @Override
    @Transactional
    public StageProgressVO updateManual(Long progressId, Integer manualScore, String manualFeedback) {
        StudentStageProgress progress = progressMapper.selectById(progressId);
        if (progress == null) throw new BizException("NOT_FOUND", "进度记录不存在，id=" + progressId);

        // 校验分数范围
        if (manualScore != null && (manualScore < 0 || manualScore > 100)) {
            throw new BizException("INVALID_PARAM", "人工评分必须在 0-100 之间");
        }

        progressMapper.update(null,
            new LambdaUpdateWrapper<StudentStageProgress>()
                .eq(StudentStageProgress::getId, progressId)
                .set(StudentStageProgress::getManualScore, manualScore != null ? manualScore.shortValue() : null)
                .set(StudentStageProgress::getManualFeedback, manualFeedback)
                .set(StudentStageProgress::getManualEditedAt, LocalDateTime.now())
        );

        log.info("人工修改进度评分，progressId={}，manualScore={}", progressId, manualScore);

        // 查询阶段信息用于返回 VO
        progress = progressMapper.selectById(progressId);
        MissionStage stage = missionStageMapper.selectById(progress.getStageId());
        List<StudentRepo> repos = studentRepoMapper.selectList(
            new LambdaQueryWrapper<StudentRepo>().eq(StudentRepo::getStudentId, progress.getStudentId())
        );
        return toVO(progress, stage, repos);
    }

    /**
     * 遍历学生所有激活仓库，拉取阶段时间窗口内的 commit（message + diff），拼接为字符串。
     */
    private String buildCommitContent(StudentProfile student, List<StudentRepo> repos, MissionStage stage) {
        StringBuilder sb = new StringBuilder();
        int totalCommits = 0;

        for (StudentRepo repo : repos) {
            // 拉取该仓库在阶段时间窗口内的 commit 列表
            List<CommitInfo> commits;
            if ("GITEE".equalsIgnoreCase(repo.getPlatform())) {
                commits = giteeApiClient.fetchCommits(repo.getRepoName(), stage.getStartTime(), stage.getDeadline());
            } else {
                commits = gitHubApiClient.fetchCommits(repo.getRepoName(), stage.getStartTime(), stage.getDeadline());
            }

            if (commits.isEmpty()) continue;

            sb.append("\n=== 仓库：").append(repo.getRepoName()).append(" ===");

            // 每个仓库最多取 5 个 commit（防止 prompt 过长）
            int limit = Math.min(commits.size(), 5);
            for (int i = 0; i < limit; i++) {
                CommitInfo commitObj = commits.get(i);
                // 两个 client 返回同一个 CommitInfo 类型，直接使用
                String sha = commitObj.sha();
                String message = commitObj.message();
                LocalDateTime time = commitObj.commitTime();

                sb.append("\n--- Commit ").append(i + 1).append(" ---");
                sb.append("\n时间：").append(time);
                sb.append("\nMessage：").append(message);

                // 拉取 diff 内容
                String diff;
                if ("GITEE".equalsIgnoreCase(repo.getPlatform())) {
                    diff = giteeApiClient.fetchCommitDiff(repo.getRepoName(), sha);
                } else {
                    diff = gitHubApiClient.fetchCommitDiff(repo.getRepoName(), sha);
                }
                if (!diff.isBlank()) {
                    sb.append("\n代码变更：\n").append(diff);
                }
                totalCommits++;
            }
        }

        if (totalCommits == 0) {
            return "（该学生在此阶段时间窗口内无任何 commit 记录）";
        }
        return sb.toString();
    }

    /**
     * 构建发给千问大模型的 Prompt。
     */
    private String buildPrompt(String studentName, String stageTitle, String taskDesc, String commitContent) {
        return """
            你是一位专业的代码评审专家，正在评估学生的编程作业完成情况。

            【学生姓名】
            %s

            【阶段名称】
            %s

            【任务要求】
            %s

            【学生提交的 commit 记录（含代码变更）】
            %s

            【评估要求】
            请严格对照任务要求，从以下维度进行评估：
            1. 功能完成度：是否实现了任务要求的核心功能
            2. 代码质量：命名规范、分层结构、注释完整性
            3. 提交规范：commit message 是否清晰描述变更内容

            【输出格式】（必须严格按此格式输出）
            分数：[0-100的整数]

            评语：
            [2-4句话的综合评价，指出完成的亮点和不足，给出具体改进建议]

            未完成项：
            [列出任务要求中未完成或不足的部分，若全部完成则写"无"]
            """.formatted(studentName, stageTitle, taskDesc, commitContent);
    }

    /**
     * 从 AI 响应文本中解析分数（0-100）。
     * 若无法解析则返回 -1 表示解析失败。
     */
    private short parseScore(String response) {
        if (response == null || response.isBlank()) return -1;
        Matcher m = SCORE_PATTERN.matcher(response);
        if (m.find()) {
            String numStr = m.group(1) != null ? m.group(1) : m.group(2);
            try {
                int score = Integer.parseInt(numStr);
                if (score >= 0 && score <= 100) return (short) score;
            } catch (NumberFormatException ignored) {}
        }
        // 兜底：直接搜索响应中的数字
        Matcher fallback = Pattern.compile("(\\d{1,3})\\s*分").matcher(response);
        if (fallback.find()) {
            try {
                int score = Integer.parseInt(fallback.group(1));
                if (score >= 0 && score <= 100) return (short) score;
            } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    /**
     * 将进度实体转换为 VO。
     */
    private StageProgressVO toVO(StudentStageProgress p, MissionStage stage, List<StudentRepo> repos) {
        StageProgressVO vo = new StageProgressVO();
        if (p != null) {
            vo.setId(p.getId());
            vo.setStudentId(p.getStudentId());
            vo.setStageId(p.getStageId());
            vo.setIsCompleted(p.getIsCompleted());
            vo.setCommitCount(p.getCommitCount());
            vo.setLastCommitTime(p.getLastCommitTime());
            vo.setAiScore(p.getAiScore() != null ? p.getAiScore().intValue() : null);
            vo.setAiFeedback(p.getAiFeedback());
            vo.setAiAnalyzedAt(p.getAiAnalyzedAt());
            vo.setManualScore(p.getManualScore() != null ? p.getManualScore().intValue() : null);
            vo.setManualFeedback(p.getManualFeedback());
            vo.setManualEditedAt(p.getManualEditedAt());
            // 关联仓库名
            if (p.getRepoId() != null) {
                repos.stream().filter(r -> r.getId().equals(p.getRepoId()))
                    .findFirst().ifPresent(r -> vo.setRepoName(r.getRepoName()));
            }
        }
        if (stage != null) {
            vo.setStageTitle(stage.getTitle());
            if (vo.getStageId() == null) vo.setStageId(stage.getId());
        }
        return vo;
    }
}
