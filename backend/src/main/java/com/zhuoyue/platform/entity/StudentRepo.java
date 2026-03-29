package com.zhuoyue.platform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生仓库实体。
 * 一个学生可关联多个仓库，支持手动录入、自动扫描、github.io 三种来源。
 */
@Data
@TableName("student_repo")
public class StudentRepo {

    /** 主键（雪花ID）。 */
    @TableId
    private Long id;

    /** 关联的学生 ID。 */
    private Long studentId;

    /** 代码托管平台：GITHUB / GITEE。 */
    private String platform;

    /**
     * 仓库全名，格式 owner/repo。
     * 例如："octocat/Hello-World"。
     */
    private String repoName;

    /**
     * 仓库来源类型：
     * - MANUAL：管理员手动录入
     * - AUTO_SCAN：自动扫描该用户所有公开仓库
     * - GITHUB_IO：自动关联的 username.github.io 仓库
     */
    private String repoType;

    /** 是否启用，停用后不参与同步和分析。 */
    private Boolean isActive;

    /** 是否为主分析仓库，AI 分析默认使用此仓库，每个学生只有一个主仓库。 */
    private Boolean isPrimary;

    /** 创建时间。 */
    private LocalDateTime createdAt;
}
