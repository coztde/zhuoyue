package com.zhuoyue.platform.service;

import com.zhuoyue.platform.vo.CozeChatVO;

/**
 * Coze 知识库问答服务接口。
 */
public interface CozeService {

    /**
     * 向 Coze 知识库 Bot 提问并获取回答。
     *
     * @param question 用户提问内容
     * @return 包含 AI 回答的 VO
     */
    CozeChatVO chat(String question);
}
