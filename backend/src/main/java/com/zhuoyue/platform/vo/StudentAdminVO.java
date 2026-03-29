package com.zhuoyue.platform.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员视角的学生列表 VO。
 */
@Data
public class StudentAdminVO {

    /** 学生 ID（Long 转 String 防精度丢失）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 真实姓名。 */
    private String realName;

    /** 平台：GITHUB / GITEE。 */
    private String platform;

    /** 平台用户名。 */
    private String platformUsername;

    /** 头像 URL。 */
    private String avatarUrl;

    /** 平台昵称。 */
    private String displayName;

    /** 账号状态：ACTIVE / DISABLED。 */
    private String activeStatus;

    /** 最后同步时间，用于管理员查看同步状态。 */
    private LocalDateTime lastSyncedAt;

    /** 已完成阶段数，辅助管理员快速了解学生进度。 */
    private Long completedStages;
}
