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
import com.zhuoyue.platform.service.GitSyncService;
import com.zhuoyue.platform.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Git 同步 Service 实现。
 * 核心流程：拉取学生仓库的 commit → 对比每个课程阶段的时间窗口 → 更新阶段完成状态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitSyncServiceImpl implements GitSyncService {

    private final StudentProfileMapper studentProfileMapper;
    private final StudentRepoMapper studentRepoMapper;
    private final StudentStageProgressMapper progressMapper;
    private final MissionStageMapper missionStageMapper;
    private final GitHubApiClient gitHubApiClient;
    private final GiteeApiClient giteeApiClient;
    private final LeaderboardService leaderboardService;

    @Override
    public void syncAll() {
        // 查询所有 ACTIVE 学生
        List<StudentProfile> students = studentProfileMapper.selectList(
            new LambdaQueryWrapper<StudentProfile>()
                .eq(StudentProfile::getActiveStatus, "ACTIVE")
        );
        log.info("开始全量同步，共 {} 名学生", students.size());
        for (StudentProfile student : students) {
            try {
                syncStudentInternal(student);
            } catch (Exception e) {
                log.error("同步学生失败，id={}，姓名={}，原因：{}", student.getId(), student.getRealName(), e.getMessage());
            }
        }
        // 同步完成后清除榜单缓存，确保下次查询拿到最新数据
        leaderboardService.evictCache();
        log.info("全量同步完成");
    }

    @Override
    public void syncStudent(Long studentId) {
        StudentProfile student = studentProfileMapper.selectById(studentId);
        if (student == null) {
            throw new BizException("NOT_FOUND", "学生不存在，id=" + studentId);
        }
        syncStudentInternal(student);
        leaderboardService.evictCache();
    }

    /**
     * 同步单个学生的所有阶段进度。
     * 遍历该学生所有激活仓库，对每个课程阶段聚合所有仓库的 commit，判断是否完成。
     */
    private void syncStudentInternal(StudentProfile student) {
        // 1. 从平台 API 同步头像和昵称
        syncUserProfile(student);

        // 2. 查询该学生所有激活仓库
        List<StudentRepo> repos = studentRepoMapper.selectList(
            new LambdaQueryWrapper<StudentRepo>()
                .eq(StudentRepo::getStudentId, student.getId())
                .eq(StudentRepo::getIsActive, true)
        );

        if (repos.isEmpty()) {
            log.warn("学生 {} 尚未配置任何仓库，跳过同步", student.getRealName());
            return;
        }

        // 3. 查询所有课程阶段
        List<MissionStage> stages = missionStageMapper.selectList(null);

        for (MissionStage stage : stages) {
            try {
                syncStageProgress(student, repos, stage);
            } catch (Exception e) {
                log.warn("同步阶段进度失败，studentId={}，stageId={}，原因：{}",
                    student.getId(), stage.getId(), e.getMessage());
            }
        }
    }

    /**
     * 同步单个学生在单个阶段的进度。
     * 聚合该学生所有仓库的 commit，只要任意仓库满足条件即视为完成。
     */
    private void syncStageProgress(StudentProfile student, List<StudentRepo> repos, MissionStage stage) {
        LocalDateTime since = stage.getStartTime();
        LocalDateTime until = stage.getDeadline();

        // 聚合所有仓库的 commit 时间，记录最优（最晚）的 commit 所属仓库
        List<LocalDateTime> allCommitTimes = new ArrayList<>();
        Long bestRepoId = null;
        LocalDateTime bestCommitTime = null;

        for (StudentRepo repo : repos) {
            List<LocalDateTime> commitTimes = fetchCommitTimesFromRepo(repo, since, until);
            if (!commitTimes.isEmpty()) {
                allCommitTimes.addAll(commitTimes);
                LocalDateTime repoLatest = commitTimes.stream().max(LocalDateTime::compareTo).orElse(null);
                if (repoLatest != null && (bestCommitTime == null || repoLatest.isAfter(bestCommitTime))) {
                    bestCommitTime = repoLatest;
                    bestRepoId = repo.getId();
                }
            }
        }

        // 判断阶段完成条件：任意仓库在时间窗口内有 commit 即完成
        boolean completed;
        if (until != null) {
            LocalDateTime effectiveSince = since != null ? since : LocalDateTime.MIN;
            completed = allCommitTimes.stream().anyMatch(
                t -> !t.isBefore(effectiveSince) && !t.isAfter(until)
            );
        } else {
            completed = !allCommitTimes.isEmpty();
        }

        LocalDateTime lastCommitTime = allCommitTimes.isEmpty() ? null
            : allCommitTimes.stream().max(LocalDateTime::compareTo).orElse(null);

        // 查询现有记录，存在则更新，不存在则插入
        StudentStageProgress existing = progressMapper.selectOne(
            new LambdaQueryWrapper<StudentStageProgress>()
                .eq(StudentStageProgress::getStudentId, student.getId())
                .eq(StudentStageProgress::getStageId, stage.getId())
        );

        if (existing == null) {
            StudentStageProgress progress = new StudentStageProgress();
            progress.setStudentId(student.getId());
            progress.setStageId(stage.getId());
            progress.setIsCompleted(completed);
            progress.setCommitCount(allCommitTimes.size());
            progress.setLastCommitTime(lastCommitTime);
            progress.setRepoId(bestRepoId);
            progress.setSyncedAt(LocalDateTime.now());
            progressMapper.insert(progress);
        } else {
            progressMapper.update(null,
                new LambdaUpdateWrapper<StudentStageProgress>()
                    .eq(StudentStageProgress::getId, existing.getId())
                    .set(StudentStageProgress::getIsCompleted, completed)
                    .set(StudentStageProgress::getCommitCount, allCommitTimes.size())
                    .set(StudentStageProgress::getLastCommitTime, lastCommitTime)
                    .set(StudentStageProgress::getRepoId, bestRepoId)
                    .set(StudentStageProgress::getSyncedAt, LocalDateTime.now())
            );
        }
    }

    /** 从指定仓库获取时间窗口内的 commit 时间列表。 */
    private List<LocalDateTime> fetchCommitTimesFromRepo(StudentRepo repo, LocalDateTime since, LocalDateTime until) {
        List<CommitInfo> commits;
        if ("GITEE".equalsIgnoreCase(repo.getPlatform())) {
            commits = giteeApiClient.fetchCommits(repo.getRepoName(), since, until);
        } else {
            commits = gitHubApiClient.fetchCommits(repo.getRepoName(), since, until);
        }
        return commits.stream()
            .map(CommitInfo::commitTime)
            .filter(t -> t != null)
            .toList();
    }

    /** 同步用户头像和昵称，写回 student_profile 表。 */
    private void syncUserProfile(StudentProfile student) {
        try {
            Map<String, Object> userInfo;
            if ("GITEE".equalsIgnoreCase(student.getPlatform())) {
                userInfo = giteeApiClient.fetchUserInfo(student.getPlatformUsername());
            } else {
                userInfo = gitHubApiClient.fetchUserInfo(student.getPlatformUsername());
            }
            if (userInfo == null || userInfo.isEmpty()) return;

            String avatarUrl = (String) userInfo.get("avatar_url");
            // GitHub 返回 name，Gitee 返回 name
            String displayName = (String) userInfo.get("name");
            if (displayName == null) displayName = (String) userInfo.get("login");

            student.setAvatarUrl(avatarUrl);
            student.setDisplayName(displayName);
            studentProfileMapper.updateById(student);
        } catch (Exception e) {
            log.warn("同步用户头像失败，studentId={}，原因：{}", student.getId(), e.getMessage());
        }
    }
}
