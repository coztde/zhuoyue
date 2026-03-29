package com.zhuoyue.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhuoyue.platform.entity.StudentRepo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学生仓库 Mapper。
 * 继承 MyBatis-Plus BaseMapper，提供基础 CRUD 操作。
 */
@Mapper
public interface StudentRepoMapper extends BaseMapper<StudentRepo> {
}
