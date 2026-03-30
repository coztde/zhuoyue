package com.zhuoyue.platform.dto;

import lombok.Data;

@Data
public class WikiLikeRequest {
    /** POST 或 COMMENT */
    private String targetType;
    private Long targetId;
    private String voterToken;
}
