package com.zhuoyue.platform.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WikiPostVO {
    private Long id;
    private String title;
    private String content;
    private String authorName;
    private Integer version;
    private String tag;

    private Integer likeCount;
    private Integer commentCount;
    private Double heatScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** 当前请求者是否已点赞（根据 voterToken 判断） */
    private Boolean liked;
    /** 当前请求者是否是作者（根据 authorToken 判断） */
    private Boolean isAuthor;
    /** 是否正被他人编辑（编辑锁未超时且非本人持有） */
    private Boolean isEditing;
    /** 最近一次编辑理由 */
    private String editReason;
}
