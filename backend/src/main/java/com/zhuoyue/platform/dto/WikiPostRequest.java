package com.zhuoyue.platform.dto;

import lombok.Data;

@Data
public class WikiPostRequest {
    private String title;
    private String content;
    private String authorName;
    private String authorToken;
    /** 标签，如：文章、问题、分享、公告 */
    private String tag;

    /** 编辑理由（编辑时建议填写） */
    private String editReason;

    /** 编辑时必须传，用于乐观锁校验 */
    private Integer version;
}
