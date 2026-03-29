package com.zhuoyue.platform.scheduler;

import com.zhuoyue.platform.service.GitSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Git 同步定时任务。
 * 每 30 分钟自动触发一次全量同步，确保榜单数据及时更新。
 * 需要在启动类加 @EnableScheduling 注解（或配置类中开启）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitSyncScheduler {

    private final GitSyncService gitSyncService;

    /**
     * 每 30 分钟执行一次全量同步。
     * fixedRate = 1800000 毫秒 = 30 分钟。
     * initialDelay = 60000 毫秒 = 启动后延迟 1 分钟再开始，避免服务刚启动时立即触发。
     */
    @Scheduled(fixedRate = 1_800_000, initialDelay = 60_000)
    public void scheduledSync() {
        log.info("[定时任务] 开始全量同步 Git commit 数据");
        try {
            gitSyncService.syncAll();
        } catch (Exception e) {
            log.error("[定时任务] 全量同步异常：{}", e.getMessage(), e);
        }
    }
}
