package com.zhuoyue.platform.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生阶段进度 VO，用于管理后台展示 AI 分析结果和人工修改入口。
 */
@Data
public class StageProgressVO {

    /** 进度记录 ID（用于人工修改接口）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 学生 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long studentId;

    /** 阶段 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long stageId;

    /** 阶段标题。 */
    private String stageTitle;

    /** 关联仓库名（owner/repo）。 */
    private String repoName;

    /** 是否已完成该阶段。 */
    private Boolean isCompleted;

    /** 时间窗口内的 commit 总数。 */
    private Integer commitCount;

    /** 最近一次 commit 时间。 */
    private LocalDateTime lastCommitTime;

    /** AI 分析得分（0-100），null 表示未分析。 */
    private Integer aiScore;

    /** AI 分析评语。 */
    private String aiFeedback;

    /** AI 分析时间。 */
    private LocalDateTime aiAnalyzedAt;

    /** 人工覆盖得分，不为 null 时优先展示此值。 */
    private Integer manualScore;

    /** 人工覆盖评语，不为 null 时优先展示此值。 */
    private String manualFeedback;

    /** 人工修改时间。 */
    private LocalDateTime manualEditedAt;

    /** 最终展示得分：有人工分则取人工分，否则取 AI 分。 */
    public Integer getEffectiveScore() {
        return manualScore != null ? manualScore : aiScore;
    }

    /** 最终展示评语：有人工评语则取人工评语，否则取 AI 评语。 */
    public String getEffectiveFeedback() {
        return manualFeedback != null ? manualFeedback : aiFeedback;
    }

    /** 是否已被人工修改过。 */
    public boolean isManualEdited() {
        return manualEditedAt != null;
    }
}
