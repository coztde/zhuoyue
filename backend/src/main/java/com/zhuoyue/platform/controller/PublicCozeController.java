package com.zhuoyue.platform.controller;

import com.zhuoyue.platform.client.CozeApiClient;
import com.zhuoyue.platform.dto.CozeChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Coze 知识库问答公开接口，无需登录，学生端直接调用。
 *
 * 流式输出：stream=true，Coze 边生成边推 token，后端实时转发给前端。
 * 中断支持：前端发 POST /api/coze/abort/{requestId}，后端 cancel Future 中断线程，
 *           CozeApiClient.streamChat 检测中断标志后停止读取 InputStream。
 * 线程管理：使用 Spring applicationTaskExecutor 线程池，不显式创建线程。
 */
@Slf4j
@RestController
@RequestMapping("/api/coze")
@RequiredArgsConstructor
public class PublicCozeController {

    private final CozeApiClient cozeApiClient;

    /**
     * Spring Boot 自动配置的线程池（applicationTaskExecutor），
     * 底层为 ThreadPoolTaskExecutor，支持提交 Callable 并返回 Future。
     */
    private final TaskExecutor taskExecutor;

    @Value("${coze.bot-id}")
    private String botId;

    /** 正在运行的任务表：requestId → Future，用于支持 cancel 中断 */
    private final ConcurrentHashMap<String, Future<?>> activeTasks = new ConcurrentHashMap<>();

    /**
     * 真流式问答接口（stream=true）。
     * 响应头 X-Request-Id 供前端发起中断请求。
     */
    @PostMapping("/chat")
    public SseEmitter chat(@RequestBody CozeChatRequest request,
                           jakarta.servlet.http.HttpServletResponse response) {
        SseEmitter emitter = new SseEmitter(120_000L);

        String requestId = UUID.randomUUID().toString().replace("-", "");
        response.setHeader("X-Request-Id", requestId);
        response.setHeader("Access-Control-Expose-Headers", "X-Request-Id");

        // AtomicBoolean 保证 emitter 只被 complete 一次，防止并发重复关闭
        AtomicBoolean completed = new AtomicBoolean(false);

        // 提交到线程池，保存 Future 以便后续 cancel
        Future<?> future = ((org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor) taskExecutor)
                .submit(() -> {
                    try {
                        String question = request.getQuestion();
                        if (!StringUtils.hasText(question)) {
                            safeSend(emitter, completed, SseEmitter.event().name("error").data("提问内容不能为空"), true);
                            return;
                        }
                        if (!StringUtils.hasText(botId)) {
                            safeSend(emitter, completed, SseEmitter.event().name("error").data("Coze Bot ID 未配置，请联系管理员"), true);
                            return;
                        }

                        String userId = "zhuoyue-" + requestId.substring(0, 12);
                        log.info("Coze 流式问答开始，requestId={}，问题长度={}", requestId, question.length());

                        cozeApiClient.streamChat(
                                botId, userId, question,
                                chunk -> {
                                    if (Thread.currentThread().isInterrupted() || completed.get()) return;
                                    safeSend(emitter, completed, SseEmitter.event().name("message").data(chunk), false);
                                },
                                () -> {
                                    log.info("Coze 流式推送完成，requestId={}", requestId);
                                    safeSend(emitter, completed, SseEmitter.event().name("done").data("[DONE]"), true);
                                },
                                errMsg -> safeSend(emitter, completed, SseEmitter.event().name("error").data(errMsg), true)
                        );

                        // streamChat 内部捕获中断后返回 null，不抛异常
                        // 需在此处补充检测，确保发送 aborted 事件
                        if (Thread.currentThread().isInterrupted()) {
                            log.info("Coze 流式输出被用户中断（streamChat 内部处理），requestId={}", requestId);
                            safeSend(emitter, completed, SseEmitter.event().name("aborted").data("[ABORTED]"), true);
                            return;
                        }

                    } catch (Exception e) {
                        if (Thread.currentThread().isInterrupted() || e.getCause() instanceof InterruptedException) {
                            log.info("Coze 流式输出被用户中断，requestId={}", requestId);
                            safeSend(emitter, completed, SseEmitter.event().name("aborted").data("[ABORTED]"), true);
                            return;
                        }
                        log.error("Coze SSE 异常，requestId={}：{}", requestId, e.getMessage(), e);
                        if (completed.compareAndSet(false, true)) {
                            emitter.completeWithError(e);
                        }
                    } finally {
                        activeTasks.remove(requestId);
                    }
                });

        activeTasks.put(requestId, future);
        return emitter;
    }

    /**
     * 中断指定请求的流式输出。
     * future.cancel(true) 会向执行线程发送中断信号，
     * CozeApiClient 的 readLine() 检测到中断后停止读取。
     */
    @PostMapping("/abort/{requestId}")
    public Map<String, Object> abort(@PathVariable String requestId) {
        Future<?> future = activeTasks.get(requestId);
        if (future != null && !future.isDone()) {
            future.cancel(true);
            log.info("已中断 Coze 流式请求，requestId={}", requestId);
            return Map.of("success", true, "message", "已中断");
        }
        return Map.of("success", false, "message", "请求不存在或已完成");
    }

    /**
     * 安全发送 SSE 事件，防止 emitter 已关闭后重复发送导致 IllegalStateException。
     */
    private void safeSend(SseEmitter emitter, AtomicBoolean completed,
                          SseEmitter.SseEventBuilder event, boolean complete) {
        if (completed.get()) return;
        try {
            emitter.send(event);
            if (complete && completed.compareAndSet(false, true)) {
                emitter.complete();
            }
        } catch (IOException | IllegalStateException ignored) {
            completed.set(true);
        }
    }
}
