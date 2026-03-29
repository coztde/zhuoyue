package com.zhuoyue.platform.controller;

import com.zhuoyue.platform.common.result.Result;
import com.zhuoyue.platform.dto.StudentSaveRequest;
import com.zhuoyue.platform.service.StudentService;
import com.zhuoyue.platform.vo.StudentAdminVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员学生档案接口。
 * 所有接口均需 Bearer Token，由 AdminAuthInterceptor 统一拦截鉴权。
 */
@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class AdminStudentController {

    private final StudentService studentService;

    /** 获取所有学生列表（含同步状态）。 */
    @GetMapping
    public Result<List<StudentAdminVO>> list() {
        return Result.success(studentService.listStudents());
    }

    /** 新增学生。 */
    @PostMapping
    public Result<StudentAdminVO> create(@RequestBody StudentSaveRequest request) {
        return Result.success(studentService.createStudent(request));
    }

    /** 编辑学生信息。 */
    @PutMapping("/{id}")
    public Result<StudentAdminVO> update(@PathVariable Long id,
                                         @RequestBody StudentSaveRequest request) {
        return Result.success(studentService.updateStudent(id, request));
    }

    /** 删除学生。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return Result.success(null);
    }
}
