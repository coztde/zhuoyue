package com.zhuoyue.platform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生阶段进度实体。
 * 每条记录表示某学生在某个课程阶段的完成状态，由 GitSyncService 定时维护。
 */
@Data
@TableName("student_stage_progress")
public class StudentStageProgress {

    /** 主键（雪花ID）。 */
    @TableId
    private Long id;

    /** 关联的学生 ID（student_profile.id）。 */
    private Long studentId;

    /** 关联的课程阶段 ID（mission_stage.id）。 */
    private Long stageId;

    /**
     * 是否已完成该阶段。
     * 完成判定逻辑：
     * - 有 deadline 的阶段：在 [start_time, deadline] 内有 ≥1 次 commit → true
     * - 无 deadline 的阶段：有任意 commit → true
     */
    private Boolean isCompleted;

    /** 时间窗口内的 commit 总数。 */
    private Integer commitCount;

    /** 最近一次 commit 的 SHA，用于 ETag 增量同步去重。 */
    private String lastCommitSha;

    /** 最近一次 commit 的时间。 */
    private LocalDateTime lastCommitTime;

    /** 最后一次同步时间。 */
    private LocalDateTime syncedAt;

    /** 关联的仓库 ID（student_repo.id），记录该进度来自哪个仓库。 */
    private Long repoId;

    /** AI 分析得分（0-100），null 表示尚未分析。 */
    private Short aiScore;

    /** AI 分析评语。 */
    private String aiFeedback;

    /** AI 分析完成时间。 */
    private LocalDateTime aiAnalyzedAt;

    /** 人工覆盖得分，不为 null 时展示时优先使用此值。 */
    private Short manualScore;

    /** 人工覆盖评语，不为 null 时展示时优先使用此值。 */
    private String manualFeedback;

    /** 人工修改时间。 */
    private LocalDateTime manualEditedAt;
}
