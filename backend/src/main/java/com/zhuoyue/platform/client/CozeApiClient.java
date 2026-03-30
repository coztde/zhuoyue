package com.zhuoyue.platform.client;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Coze 平台 API 客户端。
 * 封装三个核心操作：
 *   1. createChat   - 发起对话，拿到 chatId + conversationId
 *   2. getChatStatus - 轮询对话状态（in_progress / completed / failed）
 *   3. getChatMessages - 拉取对话消息列表，提取 AI 回答
 *
 * 认证方式：Bearer Token（PAT），通过环境变量 COZE_TOKEN 注入。
 * 参考文档：https://www.coze.cn/docs/developer_guides/chat_v3
 */
@Slf4j
@Component
public class CozeApiClient {

    /** Coze API 基础地址，国内版为 https://api.coze.cn */
    @Value("${coze.api-url}")
    private String apiUrl;

    /** Coze 个人访问令牌（PAT），用于 Authorization: Bearer 认证 */
    @Value("${coze.token}")
    private String token;

    /** 构建带认证头的 RestClient，使用 OkHttp 避免 JDK TLS 握手问题 */
    private RestClient buildClient() {
        return RestClient.builder()
                .requestFactory(new OkHttp3ClientHttpRequestFactory(new OkHttpClient()))
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * 真流式对话：stream=true，Coze 以 SSE 格式边生成边返回。
     * 解析事件类型 conversation.message.delta，提取 content 字段逐块回调。
     *
     * @param botId      Coze Bot ID
     * @param userId     用户标识
     * @param question   提问内容
     * @param onChunk    每收到一个文本块时的回调
     * @param onDone     对话完成时的回调
     * @param onError    发生错误时的回调
     */
    @SuppressWarnings("unchecked")
    public void streamChat(String botId, String userId, String question,
                           Consumer<String> onChunk, Runnable onDone, Consumer<String> onError) {
        try {
            Map<String, Object> body = Map.of(
                    "bot_id", botId,
                    "user_id", userId,
                    "stream", true,
                    "additional_messages", List.of(
                            Map.of("role", "user", "content", question, "content_type", "text")
                    )
            );

            // 用 exchange 获取原始响应流，不让 RestClient 自动反序列化
            buildClient().post()
                    .uri("/v3/chat")
                    .body(body)
                    .exchange((req, resp) -> {
                        try (InputStream is = resp.getBody();
                             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

                            String line;
                            String eventName = "";
                            // 每次读行前检测中断标志，支持 abort 接口真正停止读取
                            while (!Thread.currentThread().isInterrupted() && (line = reader.readLine()) != null) {
                                if (line.startsWith("event:")) {
                                    // 记录当前事件类型
                                    eventName = line.substring(6).trim();
                                } else if (line.startsWith("data:")) {
                                    String data = line.substring(5).trim();
                                    if ("[DONE]".equals(data)) break;

                                    // 只处理消息增量事件
                                    if ("conversation.message.delta".equals(eventName)) {
                                        // data 是 JSON，取 content 字段
                                        String content = extractJsonField(data, "content");
                                        if (content != null && !content.isEmpty()) {
                                            onChunk.accept(content);
                                        }
                                    } else if ("conversation.chat.completed".equals(eventName)) {
                                        onDone.run();
                                    } else if ("conversation.chat.failed".equals(eventName)) {
                                        onError.accept("AI 回答失败，请稍后重试");
                                        return null;
                                    }
                                }
                            }
                            // 正常结束（非中断）才触发 onDone
                            if (!Thread.currentThread().isInterrupted()) {
                                onDone.run();
                            }
                        } catch (Exception e) {
                            if (e.getCause() instanceof InterruptedException) {
                                // 被 abort 主动中断，恢复中断标志后直接返回，不触发 onError
                                Thread.currentThread().interrupt();
                                return null;
                            }
                            log.error("Coze 流式读取异常：{}", e.getMessage(), e);
                            onError.accept("流式读取异常：" + e.getMessage());
                        }
                        return null;
                    });

        } catch (Exception e) {
            log.error("Coze streamChat 异常：{}", e.getMessage(), e);
            onError.accept("请求异常：" + e.getMessage());
        }
    }

    /**
     * 从 JSON 字符串中简单提取指定字段值（避免引入额外 JSON 解析依赖）。
     * 仅用于提取简单字符串字段，格式："key":"value"
     */
    private String extractJsonField(String json, String field) {
        try {
            String key = "\"" + field + "\"";
            int idx = json.indexOf(key);
            if (idx < 0) return null;
            int colon = json.indexOf(':', idx + key.length());
            if (colon < 0) return null;
            // 跳过空白
            int start = colon + 1;
            while (start < json.length() && json.charAt(start) == ' ') start++;
            if (start >= json.length()) return null;
            if (json.charAt(start) == '"') {
                // 字符串值，找结束引号（处理转义）
                StringBuilder sb = new StringBuilder();
                int i = start + 1;
                while (i < json.length()) {
                    char c = json.charAt(i);
                    if (c == '\\' && i + 1 < json.length()) {
                        char next = json.charAt(i + 1);
                        if (next == 'n') sb.append('\n');
                        else if (next == 't') sb.append('\t');
                        else sb.append(next);
                        i += 2;
                    } else if (c == '"') {
                        break;
                    } else {
                        sb.append(c);
                        i++;
                    }
                }
                return sb.toString();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 发起一次对话请求。
     *
     * @param botId  Coze Bot ID
     * @param userId 用户标识（任意字符串，用于区分会话）
     * @param question 用户提问内容
     * @return 包含 id（chatId）和 conversation_id 的 Map，失败返回 null
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> createChat(String botId, String userId, String question) {
        try {
            // 构造请求体：bot_id、user_id、问题消息列表，auto_save_history=false 避免存储历史
            Map<String, Object> body = Map.of(
                    "bot_id", botId,
                    "user_id", userId,
                    "stream", false,
                    "additional_messages", List.of(
                            Map.of("role", "user", "content", question, "content_type", "text")
                    )
            );

            Map<String, Object> response = buildClient().post()
                    .uri("/v3/chat")
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response == null) {
                log.warn("Coze createChat 返回空响应");
                return null;
            }

            // Coze v3 响应格式：{ code: 0, data: { id, conversation_id, status, ... } }
            Integer code = (Integer) response.get("code");
            if (code == null || code != 0) {
                log.warn("Coze createChat 失败，code={}，msg={}", code, response.get("msg"));
                return null;
            }

            return (Map<String, Object>) response.get("data");

        } catch (Exception e) {
            log.error("Coze createChat 异常：{}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 查询对话状态。
     *
     * @param conversationId 会话 ID
     * @param chatId         对话 ID
     * @return 状态字符串：in_progress / completed / failed / requires_action，异常返回 "error"
     */
    @SuppressWarnings("unchecked")
    public String getChatStatus(String conversationId, String chatId) {
        try {
            Map<String, Object> response = buildClient().get()
                    .uri("/v3/chat/retrieve?conversation_id={cid}&chat_id={id}", conversationId, chatId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response == null) return "error";

            Integer code = (Integer) response.get("code");
            if (code == null || code != 0) {
                log.warn("Coze getChatStatus 失败，code={}，msg={}", code, response.get("msg"));
                return "error";
            }

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            return data != null ? (String) data.get("status") : "error";

        } catch (Exception e) {
            log.error("Coze getChatStatus 异常：{}", e.getMessage(), e);
            return "error";
        }
    }

    /**
     * 获取对话消息列表，提取 AI 回答文本。
     *
     * @param conversationId 会话 ID
     * @param chatId         对话 ID
     * @return AI 回答内容，未找到时返回空字符串
     */
    @SuppressWarnings("unchecked")
    public String getChatAnswer(String conversationId, String chatId) {
        try {
            Map<String, Object> response = buildClient().get()
                    .uri("/v3/chat/message/list?conversation_id={cid}&chat_id={id}", conversationId, chatId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response == null) return "";

            Integer code = (Integer) response.get("code");
            if (code == null || code != 0) {
                log.warn("Coze getChatAnswer 失败，code={}，msg={}", code, response.get("msg"));
                return "";
            }

            // 消息列表中找 role=assistant、type=answer 的那条
            List<Map<String, Object>> messages = (List<Map<String, Object>>) response.get("data");
            if (messages == null || messages.isEmpty()) return "";

            return messages.stream()
                    .filter(m -> "assistant".equals(m.get("role")) && "answer".equals(m.get("type")))
                    .map(m -> (String) m.get("content"))
                    .findFirst()
                    .orElse("");

        } catch (Exception e) {
            log.error("Coze getChatAnswer 异常：{}", e.getMessage(), e);
            return "";
        }
    }
}
