package com.zhuoyue.platform.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 排行榜单条目 VO，供公开接口返回。
 */
@Data
public class LeaderboardItemVO {

    /** 学生 ID（Long 转 String，防止 JS 精度丢失）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 真实姓名。 */
    private String realName;

    /** 平台昵称（可能为空，前端降级显示 realName）。 */
    private String displayName;

    /** 头像 URL。 */
    private String avatarUrl;

    /** 已完成的阶段数。 */
    private Long completedStages;

    /** 课程总阶段数。 */
    private Long totalStages;

    /** 最近一次 commit 时间，用于同分排序。 */
    private LocalDateTime latestCommit;

    /** 最后同步时间，前端用于展示「最后同步于 xx」。 */
    private LocalDateTime lastSyncedAt;
}
