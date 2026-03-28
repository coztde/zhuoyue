package com.zhuoyue.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zhuoyue.platform.common.exception.BizException;
import com.zhuoyue.platform.dto.CourseSaveRequest;
import com.zhuoyue.platform.entity.MissionStage;
import com.zhuoyue.platform.mapper.MissionStageMapper;
import com.zhuoyue.platform.service.CourseManagementService;
import com.zhuoyue.platform.vo.AdminCourseVO;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * 课程展览后台管理服务。
 */
@Service
public class CourseManagementServiceImpl implements CourseManagementService {

    private static final int MAX_TITLE_LENGTH = 128;

    private final MissionStageMapper missionStageMapper;

    public CourseManagementServiceImpl(MissionStageMapper missionStageMapper) {
        this.missionStageMapper = missionStageMapper;
    }

    @Override
    public List<AdminCourseVO> listCourses() {
        List<MissionStage> stages = missionStageMapper.selectList(new LambdaQueryWrapper<MissionStage>()
                .orderByAsc(MissionStage::getStageOrder));
        return stages.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminCourseVO createCourse(CourseSaveRequest request) {
        validateRequest(request, null);
        MissionStage stage = new MissionStage();
        stage.setId(IdWorker.getId());
        applyStageRequest(stage, request);
        missionStageMapper.insert(stage);
        return toVO(stage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminCourseVO updateCourse(Long courseId, CourseSaveRequest request) {
        validateRequest(request, courseId);
        MissionStage stage = missionStageMapper.selectById(courseId);
        if (stage == null) {
            throw new BizException("COURSE_NOT_FOUND", "课程阶段不存在");
        }
        applyStageRequest(stage, request);
        missionStageMapper.updateById(stage);
        return toVO(stage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourse(Long courseId) {
        MissionStage stage = missionStageMapper.selectById(courseId);
        if (stage == null) {
            throw new BizException("COURSE_NOT_FOUND", "课程阶段不存在");
        }
        missionStageMapper.deleteById(courseId);
    }

    private void validateRequest(CourseSaveRequest request, Long currentCourseId) {
        if (request == null || !StringUtils.hasText(request.getTitle())) {
            throw new BizException("COURSE_TITLE_EMPTY", "课程标题不能为空");
        }
        if (!StringUtils.hasText(request.getTaskTitle())) {
            throw new BizException("COURSE_TASK_TITLE_EMPTY", "阶段任务不能为空");
        }

        String normalizedTitle = request.getTitle().trim();
        if (normalizedTitle.length() > MAX_TITLE_LENGTH) {
            throw new BizException("COURSE_TITLE_TOO_LONG", "课程标题过长，请控制在 128 个字符以内");
        }
        if (request.getStageOrder() == null || request.getStageOrder() < 1) {
            throw new BizException("COURSE_STAGE_ORDER_INVALID", "阶段顺序必须大于 0");
        }

        LambdaQueryWrapper<MissionStage> duplicateWrapper = new LambdaQueryWrapper<MissionStage>()
                .eq(MissionStage::getStageOrder, request.getStageOrder());
        if (currentCourseId != null) {
            duplicateWrapper.ne(MissionStage::getId, currentCourseId);
        }
        Long duplicateCount = missionStageMapper.selectCount(duplicateWrapper);
        if (duplicateCount != null && duplicateCount > 0) {
            throw new BizException("COURSE_STAGE_ORDER_DUPLICATE", "这个阶段顺序已经存在，请换一个阶段顺序");
        }
    }

    private void applyStageRequest(MissionStage stage, CourseSaveRequest request) {
        stage.setTitle(request.getTitle().trim());
        stage.setTaskTitle(request.getTaskTitle().trim());
        stage.setStageOrder(request.getStageOrder() == null ? 0 : request.getStageOrder());
        if (!StringUtils.hasText(stage.getCode())) {
            stage.setCode(buildStageCode(stage.getId(), request.getTitle(), request.getStageOrder()));
        }
    }

    private String buildStageCode(Long stageId, String title, Integer stageOrder) {
        String normalizedTitle = title == null ? "" : title.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("^-+|-+$", "");
        String titlePart = StringUtils.hasText(normalizedTitle) ? normalizedTitle : "course-stage";
        int order = stageOrder == null ? 0 : stageOrder;
        return titlePart + "-" + order + "-" + stageId;
    }

    private AdminCourseVO toVO(MissionStage stage) {
        return AdminCourseVO.builder()
                .id(stage.getId())
                .title(stage.getTitle())
                .taskTitle(stage.getTaskTitle())
                .stageOrder(stage.getStageOrder())
                .build();
    }
}
