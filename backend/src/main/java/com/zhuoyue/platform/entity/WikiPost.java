package com.zhuoyue.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wiki_post")
public class WikiPost {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private String authorName;

    private String authorToken;

    private Integer version;

    private Integer likeCount;

    private Integer commentCount;

    private Integer reportCount;

    private Double heatScore;

    /** 标签，如：文章、问题、分享、公告 */
    private String tag;

    /** 编辑锁：当前正在编辑的用户 token，null 表示无人编辑 */
    private String editingByToken;

    /** 编辑锁开始时间，超过 5 分钟自动失效 */
    private LocalDateTime editingSince;

    /** 最近一次编辑理由 */
    private String editReason;

    private Boolean isHidden;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
