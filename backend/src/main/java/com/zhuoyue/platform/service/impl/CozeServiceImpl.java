package com.zhuoyue.platform.service.impl;

import com.zhuoyue.platform.client.CozeApiClient;
import com.zhuoyue.platform.common.exception.BizException;
import com.zhuoyue.platform.service.CozeService;
import com.zhuoyue.platform.vo.CozeChatVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;

/**
 * Coze 知识库问答服务实现。
 *
 * 调用流程（Coze v3/chat 为异步接口）：
 *   1. createChat  → 拿到 chatId + conversationId
 *   2. 轮询 getChatStatus 至 completed（最多 pollMaxTimes 次，间隔 pollIntervalMs 毫秒）
 *   3. getChatAnswer → 提取 AI 回答文本
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CozeServiceImpl implements CozeService {

    private final CozeApiClient cozeApiClient;

    /** Coze Bot ID，通过环境变量 COZE_BOT_ID 注入 */
    @Value("${coze.bot-id}")
    private String botId;

    /** 最多轮询次数，防止无限等待 */
    @Value("${coze.poll-max-times:20}")
    private int pollMaxTimes;

    /** 每次轮询间隔（毫秒） */
    @Value("${coze.poll-interval-ms:1000}")
    private long pollIntervalMs;

    @Override
    public CozeChatVO chat(String question) {
        // 参数校验
        if (!StringUtils.hasText(question)) {
            throw new BizException("400", "提问内容不能为空");
        }
        if (!StringUtils.hasText(botId)) {
            throw new BizException("500", "Coze Bot ID 未配置，请联系管理员");
        }

        // 每次对话生成一个唯一 userId，避免会话串扰
        String userId = "zhuoyue-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        log.info("发起 Coze 问答，botId={}，userId={}，问题长度={}", botId, userId, question.length());

        // 第一步：发起对话
        Map<String, Object> chatData = cozeApiClient.createChat(botId, userId, question);
        if (chatData == null) {
            throw new BizException("500", "Coze 对话创建失败，请稍后重试");
        }

        String chatId = (String) chatData.get("id");
        String conversationId = (String) chatData.get("conversation_id");
        log.debug("Coze 对话已创建，chatId={}，conversationId={}", chatId, conversationId);

        // 第二步：轮询状态，等待 completed
        String status = (String) chatData.get("status");
        for (int i = 0; i < pollMaxTimes; i++) {
            // 如果创建时已经是 completed 则跳过轮询（极少情况）
            if ("completed".equals(status)) {
                break;
            }
            if ("failed".equals(status) || "requires_action".equals(status) || "error".equals(status)) {
                log.warn("Coze 对话异常终止，status={}，chatId={}", status, chatId);
                throw new BizException("500", "AI 回答失败，请稍后重试");
            }

            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BizException("500", "请求被中断");
            }

            status = cozeApiClient.getChatStatus(conversationId, chatId);
            log.debug("Coze 轮询第 {} 次，status={}", i + 1, status);
        }

        if (!"completed".equals(status)) {
            log.warn("Coze 对话超时未完成，chatId={}，最终 status={}", chatId, status);
            throw new BizException("500", "AI 响应超时，请稍后重试");
        }

        // 第三步：获取回答内容
        String answer = cozeApiClient.getChatAnswer(conversationId, chatId);
        if (!StringUtils.hasText(answer)) {
            throw new BizException("500", "未获取到 AI 回答，请稍后重试");
        }

        log.info("Coze 问答完成，chatId={}，回答长度={}", chatId, answer.length());
        return new CozeChatVO(answer);
    }
}
