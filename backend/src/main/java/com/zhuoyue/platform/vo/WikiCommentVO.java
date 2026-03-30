package com.zhuoyue.platform.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WikiCommentVO {
    private Long id;
    private Long postId;
    private Long parentId;
    private String authorName;
    private String content;
    private Integer likeCount;
    private LocalDateTime createdAt;
    private Boolean liked;
    private Boolean isAuthor;
    /** 子评论列表（仅顶层评论携带） */
    private List<WikiCommentVO> children;
}
