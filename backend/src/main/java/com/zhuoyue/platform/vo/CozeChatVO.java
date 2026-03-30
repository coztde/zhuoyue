package com.zhuoyue.platform.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Coze 知识库问答返回给前端的 VO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CozeChatVO {

    /** AI 回答内容。 */
    private String answer;
}
