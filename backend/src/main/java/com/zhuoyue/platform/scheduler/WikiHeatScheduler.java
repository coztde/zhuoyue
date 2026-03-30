package com.zhuoyue.platform.scheduler;

import com.zhuoyue.platform.service.WikiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Wiki 热度定时刷新，每 10 分钟更新一次所有文章的 heat_score。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WikiHeatScheduler {

    private final WikiService wikiService;

    @Scheduled(fixedDelay = 10 * 60 * 1000)
    public void refreshHeat() {
        log.info("[Wiki] 开始刷新热度...");
        wikiService.refreshHeatScores();
    }
}
