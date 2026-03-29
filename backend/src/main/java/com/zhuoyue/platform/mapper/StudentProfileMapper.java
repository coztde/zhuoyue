package com.zhuoyue.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhuoyue.platform.entity.StudentProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学生档案 Mapper。
 * 基础 CRUD 由 MyBatis-Plus BaseMapper 自动提供。
 */
@Mapper
public interface StudentProfileMapper extends BaseMapper<StudentProfile> {
}
