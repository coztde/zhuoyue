package com.zhuoyue.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wiki_comment")
public class WikiComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    private Long parentId;

    private String authorName;

    private String authorToken;

    private String content;

    private Integer likeCount;

    private Integer reportCount;

    private Boolean isHidden;

    private LocalDateTime createdAt;
}
