package com.zhuoyue.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhuoyue.platform.client.GitHubApiClient;
import com.zhuoyue.platform.client.GiteeApiClient;
import com.zhuoyue.platform.common.exception.BizException;
import com.zhuoyue.platform.entity.StudentProfile;
import com.zhuoyue.platform.entity.StudentRepo;
import com.zhuoyue.platform.mapper.StudentProfileMapper;
import com.zhuoyue.platform.mapper.StudentRepoMapper;
import com.zhuoyue.platform.service.StudentRepoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 学生仓库管理 Service 实现。
 * 支持手动添加、一键扫描所有公开仓库、自动关联 github.io/gitee.io 主页仓库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentRepoServiceImpl implements StudentRepoService {

    private final StudentRepoMapper studentRepoMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final GitHubApiClient gitHubApiClient;
    private final GiteeApiClient giteeApiClient;

    @Override
    public List<StudentRepo> listRepos(Long studentId) {
        return studentRepoMapper.selectList(
            new LambdaQueryWrapper<StudentRepo>()
                .eq(StudentRepo::getStudentId, studentId)
                .orderByAsc(StudentRepo::getCreatedAt)
        );
    }

    @Override
    @Transactional
    public StudentRepo addManualRepo(Long studentId, String repoName, String platform) {
        // 检查学生存在
        requireStudent(studentId);
        // 校验平台
        String upperPlatform = validatePlatform(platform);
        // 校验仓库名格式 owner/repo
        if (repoName == null || !repoName.contains("/")) {
            throw new BizException("INVALID_PARAM", "仓库名格式应为 owner/repo，例如：octocat/Hello-World");
        }
        return saveRepo(studentId, upperPlatform, repoName.trim(), "MANUAL");
    }

    @Override
    @Transactional
    public int scanAllPublicRepos(Long studentId) {
        StudentProfile student = requireStudent(studentId);
        String username = student.getPlatformUsername();
        String platform = student.getPlatform();

        // 调用对应平台 API 获取所有公开仓库
        List<String> allRepos;
        if ("GITEE".equalsIgnoreCase(platform)) {
            allRepos = giteeApiClient.fetchAllPublicRepos(username);
        } else {
            allRepos = gitHubApiClient.fetchAllPublicRepos(username);
        }

        if (allRepos.isEmpty()) {
            log.info("扫描用户 {} 的公开仓库为空", username);
            return 0;
        }

        // 获取已存在的仓库名集合，用于去重
        Set<String> existingRepos = studentRepoMapper.selectList(
            new LambdaQueryWrapper<StudentRepo>().eq(StudentRepo::getStudentId, studentId)
        ).stream().map(StudentRepo::getRepoName).collect(Collectors.toSet());

        // 过滤已存在的，只新增没有的
        int added = 0;
        for (String repoName : allRepos) {
            if (!existingRepos.contains(repoName)) {
                saveRepo(studentId, platform, repoName, "AUTO_SCAN");
                added++;
            }
        }
        log.info("扫描完成，用户={}，共发现 {} 个仓库，新增 {} 个", username, allRepos.size(), added);
        return added;
    }

    @Override
    @Transactional
    public StudentRepo autoLinkPageRepo(Long studentId) {
        StudentProfile student = requireStudent(studentId);
        String username = student.getPlatformUsername();
        String platform = student.getPlatform();

        // github.io 仓库名固定格式：username/username.github.io
        // gitee.io  仓库名固定格式：username/username.gitee.io
        String suffix = "GITEE".equalsIgnoreCase(platform) ? ".gitee.io" : ".github.io";
        String repoName = username + "/" + username + suffix;

        // 检查是否已存在
        Long exists = studentRepoMapper.selectCount(
            new LambdaQueryWrapper<StudentRepo>()
                .eq(StudentRepo::getStudentId, studentId)
                .eq(StudentRepo::getRepoName, repoName)
        );
        if (exists > 0) {
            log.info("主页仓库 {} 已存在，跳过", repoName);
            return studentRepoMapper.selectOne(
                new LambdaQueryWrapper<StudentRepo>()
                    .eq(StudentRepo::getStudentId, studentId)
                    .eq(StudentRepo::getRepoName, repoName)
            );
        }
        log.info("自动关联主页仓库：{}", repoName);
        return saveRepo(studentId, platform, repoName, "GITHUB_IO");
    }

    @Override
    @Transactional
    public StudentRepo setPrimary(Long studentId, Long repoId) {
        // 先将该学生所有仓库的 is_primary 置为 false
        studentRepoMapper.selectList(
            new LambdaQueryWrapper<StudentRepo>().eq(StudentRepo::getStudentId, studentId)
        ).forEach(r -> {
            if (Boolean.TRUE.equals(r.getIsPrimary())) {
                r.setIsPrimary(false);
                studentRepoMapper.updateById(r);
            }
        });
        // 再将目标仓库设为主仓库
        StudentRepo repo = studentRepoMapper.selectById(repoId);
        if (repo == null) throw new BizException("NOT_FOUND", "仓库不存在，id=" + repoId);
        repo.setIsPrimary(true);
        studentRepoMapper.updateById(repo);
        log.info("设置主仓库：studentId={}，repoId={}，repoName={}", studentId, repoId, repo.getRepoName());
        return repo;
    }

    @Override
    @Transactional
    public void deleteRepo(Long repoId) {
        // student_stage_progress.repo_id 设置了 ON DELETE SET NULL，无需手动处理进度
        int rows = studentRepoMapper.deleteById(repoId);
        if (rows == 0) {
            throw new BizException("NOT_FOUND", "仓库不存在，id=" + repoId);
        }
        log.info("删除仓库：id={}", repoId);
    }

    /**
     * 保存仓库记录，若已存在（UNIQUE 约束）则跳过并返回现有记录。
     */
    private StudentRepo saveRepo(Long studentId, String platform, String repoName, String repoType) {
        // 先查是否已存在（防止并发重复）
        StudentRepo existing = studentRepoMapper.selectOne(
            new LambdaQueryWrapper<StudentRepo>()
                .eq(StudentRepo::getStudentId, studentId)
                .eq(StudentRepo::getRepoName, repoName)
        );
        if (existing != null) return existing;

        StudentRepo repo = new StudentRepo();
        repo.setStudentId(studentId);
        repo.setPlatform(platform);
        repo.setRepoName(repoName);
        repo.setRepoType(repoType);
        repo.setIsActive(true);
        repo.setCreatedAt(LocalDateTime.now());
        studentRepoMapper.insert(repo);
        return repo;
    }

    /** 查询学生，不存在则抛出业务异常。 */
    private StudentProfile requireStudent(Long studentId) {
        StudentProfile student = studentProfileMapper.selectById(studentId);
        if (student == null) {
            throw new BizException("NOT_FOUND", "学生不存在，id=" + studentId);
        }
        return student;
    }

    /** 校验并返回大写平台名。 */
    private String validatePlatform(String platform) {
        if (platform == null || (!platform.equalsIgnoreCase("GITHUB") && !platform.equalsIgnoreCase("GITEE"))) {
            throw new BizException("INVALID_PARAM", "平台必须为 GITHUB 或 GITEE");
        }
        return platform.toUpperCase();
    }
}
