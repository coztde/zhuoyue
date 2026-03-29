package com.zhuoyue.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhuoyue.platform.common.exception.BizException;
import com.zhuoyue.platform.dto.StudentSaveRequest;
import com.zhuoyue.platform.entity.StudentProfile;
import com.zhuoyue.platform.mapper.StudentProfileMapper;
import com.zhuoyue.platform.mapper.StudentStageProgressMapper;
import com.zhuoyue.platform.service.StudentService;
import com.zhuoyue.platform.vo.LeaderboardItemVO;
import com.zhuoyue.platform.vo.StudentAdminVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学生档案管理 Service 实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentProfileMapper studentProfileMapper;
    private final StudentStageProgressMapper progressMapper;

    @Override
    public List<StudentAdminVO> listStudents() {
        // 查询所有学生（按创建时间升序）
        List<StudentProfile> profiles = studentProfileMapper.selectList(
            new LambdaQueryWrapper<StudentProfile>().orderByAsc(StudentProfile::getCreatedAt)
        );

        // 查询每位学生的完成阶段数和最后同步时间（复用全员榜单查询）
        List<LeaderboardItemVO> fullBoard = progressMapper.queryFull();
        Map<Long, LeaderboardItemVO> boardMap = fullBoard.stream()
            .collect(Collectors.toMap(LeaderboardItemVO::getId, v -> v));

        return profiles.stream().map(p -> {
            StudentAdminVO vo = new StudentAdminVO();
            vo.setId(p.getId());
            vo.setRealName(p.getRealName());
            vo.setPlatform(p.getPlatform());
            vo.setPlatformUsername(p.getPlatformUsername());
            vo.setAvatarUrl(p.getAvatarUrl());
            vo.setDisplayName(p.getDisplayName());
            vo.setActiveStatus(p.getActiveStatus());
            // 从榜单聚合数据中补充进度信息
            LeaderboardItemVO boardItem = boardMap.get(p.getId());
            if (boardItem != null) {
                vo.setCompletedStages(boardItem.getCompletedStages());
                vo.setLastSyncedAt(boardItem.getLastSyncedAt());
            } else {
                vo.setCompletedStages(0L);
            }
            return vo;
        }).toList();
    }

    @Override
    @Transactional
    public StudentAdminVO createStudent(StudentSaveRequest request) {
        validatePlatform(request.getPlatform());

        StudentProfile profile = new StudentProfile();
        profile.setRealName(request.getRealName());
        profile.setPlatform(request.getPlatform().toUpperCase());
        profile.setPlatformUsername(request.getPlatformUsername());
        profile.setActiveStatus("ACTIVE");
        profile.setCreatedAt(LocalDateTime.now());
        studentProfileMapper.insert(profile);

        log.info("新增学生：{}，平台：{}", profile.getRealName(), profile.getPlatform());
        return toAdminVO(profile);
    }

    @Override
    @Transactional
    public StudentAdminVO updateStudent(Long studentId, StudentSaveRequest request) {
        StudentProfile profile = studentProfileMapper.selectById(studentId);
        if (profile == null) {
            throw new BizException("NOT_FOUND", "学生不存在，id=" + studentId);
        }
        validatePlatform(request.getPlatform());

        profile.setRealName(request.getRealName());
        profile.setPlatform(request.getPlatform().toUpperCase());
        profile.setPlatformUsername(request.getPlatformUsername());
        studentProfileMapper.updateById(profile);

        log.info("编辑学生：id={}", studentId);
        return toAdminVO(profile);
    }

    @Override
    @Transactional
    public void deleteStudent(Long studentId) {
        if (studentProfileMapper.selectById(studentId) == null) {
            throw new BizException("NOT_FOUND", "学生不存在，id=" + studentId);
        }
        // student_stage_progress 设置了 ON DELETE CASCADE，无需手动删除进度
        studentProfileMapper.deleteById(studentId);
        log.info("删除学生：id={}", studentId);
    }

    /** 校验平台值合法性。 */
    private void validatePlatform(String platform) {
        if (platform == null || (!platform.equalsIgnoreCase("GITHUB") && !platform.equalsIgnoreCase("GITEE"))) {
            throw new BizException("INVALID_PARAM", "平台必须为 GITHUB 或 GITEE");
        }
    }

    /** 将实体转换为管理员 VO（不含聚合字段）。 */
    private StudentAdminVO toAdminVO(StudentProfile p) {
        StudentAdminVO vo = new StudentAdminVO();
        vo.setId(p.getId());
        vo.setRealName(p.getRealName());
        vo.setPlatform(p.getPlatform());
        vo.setPlatformUsername(p.getPlatformUsername());
        vo.setAvatarUrl(p.getAvatarUrl());
        vo.setDisplayName(p.getDisplayName());
        vo.setActiveStatus(p.getActiveStatus());
        vo.setCompletedStages(0L);
        return vo;
    }
}
