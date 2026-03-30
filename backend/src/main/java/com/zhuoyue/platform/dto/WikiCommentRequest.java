package com.zhuoyue.platform.dto;

import lombok.Data;

@Data
public class WikiCommentRequest {
    private Long parentId;
    private String authorName;
    private String authorToken;
    private String content;
}
