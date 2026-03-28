package com.zhuoyue.platform.service;

import com.zhuoyue.platform.dto.CourseSaveRequest;
import com.zhuoyue.platform.vo.AdminCourseVO;

import java.util.List;

/**
 * 课程展览后台管理服务。
 */
public interface CourseManagementService {

    List<AdminCourseVO> listCourses();

    AdminCourseVO createCourse(CourseSaveRequest request);

    AdminCourseVO updateCourse(Long courseId, CourseSaveRequest request);

    void deleteCourse(Long courseId);
}
