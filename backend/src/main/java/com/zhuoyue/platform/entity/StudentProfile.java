package com.zhuoyue.platform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生档案实体。
 * 存储管理员录入的学生 GitHub/Gitee 账号信息，用于定时同步 commit 并参与排行榜计算。
 */
@Data
@TableName("student_profile")
public class StudentProfile {

    /** 主键（雪花ID）。 */
    @TableId
    private Long id;

    /** 真实姓名。 */
    private String realName;

    /**
     * 代码托管平台，枚举值：GITHUB / GITEE。
     */
    private String platform;

    /** 平台用户名（如 GitHub username）。 */
    private String platformUsername;

    /** 头像 URL，同步时从平台 API 缓存，避免前端频繁跨域请求。 */
    private String avatarUrl;

    /** 平台昵称，同步时从 API 缓存。 */
    private String displayName;

    /**
     * 账号状态：ACTIVE（启用）/ DISABLED（停用）。
     * 停用的学生不参与同步和排行榜。
     */
    private String activeStatus;

    /** 创建时间。 */
    private LocalDateTime createdAt;
}
