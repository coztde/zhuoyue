package com.zhuoyue.platform.controller;

import com.zhuoyue.platform.common.result.Result;
import com.zhuoyue.platform.dto.CourseSaveRequest;
import com.zhuoyue.platform.service.CourseManagementService;
import com.zhuoyue.platform.vo.AdminCourseVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台课程展览管理接口。
 */
@RestController
@RequestMapping("/api/admin/courses")
public class AdminCourseController {

    private final CourseManagementService courseManagementService;

    public AdminCourseController(CourseManagementService courseManagementService) {
        this.courseManagementService = courseManagementService;
    }

    @GetMapping
    public Result<List<AdminCourseVO>> listCourses() {
        return Result.success(courseManagementService.listCourses());
    }

    @PostMapping
    public Result<AdminCourseVO> createCourse(@RequestBody CourseSaveRequest request) {
        return Result.success(courseManagementService.createCourse(request));
    }

    @PutMapping("/{courseId}")
    public Result<AdminCourseVO> updateCourse(@PathVariable Long courseId, @RequestBody CourseSaveRequest request) {
        return Result.success(courseManagementService.updateCourse(courseId, request));
    }

    @DeleteMapping("/{courseId}")
    public Result<Void> deleteCourse(@PathVariable Long courseId) {
        courseManagementService.deleteCourse(courseId);
        return Result.success(null);
    }
}
