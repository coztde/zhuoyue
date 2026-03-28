package com.zhuoyue.platform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员账号实体。
 * 当前平台只开放管理员后台，因此权限模型保持最小化：
 * 一个管理员账号表 + 一个登录态校验拦截器即可满足一期需求。
 */
@Data
@TableName("admin_user")
public class AdminUser {

    /**
     * 管理员主键。
     */
    @TableId
    private Long id;

    /**
     * 登录用户名。
     */
    private String username;

    /**
     * 加密后的密码摘要。
     */
    private String passwordHash;

    /**
     * 展示名称。
     */
    private String displayName;

    /**
     * 账号状态。
     * 例如 ACTIVE / DISABLED。
     */
    private String activeStatus;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
