package com.zhuoyue.platform.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WikiReportVO {
    private Long id;
    private String targetType;
    private Long targetId;
    /** COMMENT 类型时关联的文章ID，方便前端跳转 */
    private Long postId;
    private String reason;
    private String reporterToken;
    private Boolean isHandled;
    private LocalDateTime createdAt;
}
