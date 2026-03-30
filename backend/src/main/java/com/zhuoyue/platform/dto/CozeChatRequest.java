package com.zhuoyue.platform.dto;

import lombok.Data;

/**
 * 前端向后端发起 Coze 知识库问答请求的 DTO。
 */
@Data
public class CozeChatRequest {

    /** 用户提问内容，不能为空。 */
    private String question;
}
