package com.zhuoyue.platform.dto;

import lombok.Data;

@Data
public class WikiReportRequest {
    private String targetType;
    private Long targetId;
    private String reason;
    private String reporterToken;
}
