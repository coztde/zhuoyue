package com.zhuoyue.platform.service;

import com.zhuoyue.platform.dto.StudentSaveRequest;
import com.zhuoyue.platform.vo.StudentAdminVO;

import java.util.List;

/**
 * 学生档案管理 Service 接口。
 */
public interface StudentService {

    /** 获取所有学生列表（管理员视角，含同步状态）。 */
    List<StudentAdminVO> listStudents();

    /** 新增学生。 */
    StudentAdminVO createStudent(StudentSaveRequest request);

    /** 编辑学生信息。 */
    StudentAdminVO updateStudent(Long studentId, StudentSaveRequest request);

    /** 删除学生（级联删除其阶段进度）。 */
    void deleteStudent(Long studentId);
}
