package com.zhuoyue.platform.scheduler;

import com.zhuoyue.platform.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * AI 分析定时任务。
 * 每天凌晨 2 点自动触发全量分析，对所有学生所有阶段进行代码质量评估。
 * 24 小时内已分析过的记录会自动跳过，避免重复消耗 token。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiAnalysisScheduler {

    private final AiAnalysisService aiAnalysisService;

    /**
     * 每天凌晨 2:00 执行全量 AI 分析。
     * cron 表达式：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledAnalyze() {
        log.info("[AI定时分析] 开始每日全量 AI 代码分析");
        try {
            aiAnalysisService.analyzeAll();
        } catch (Exception e) {
            log.error("[AI定时分析] 全量分析异常：{}", e.getMessage(), e);
        }
    }
}
