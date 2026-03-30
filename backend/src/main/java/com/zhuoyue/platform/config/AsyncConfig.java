package com.zhuoyue.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务线程池配置。
 * 用于 Coze 流式问答等需要异步执行的场景，替代显式 new Thread()。
 * 核心线程数 5，最大 20，队列容量 50，超出时拒绝并抛异常。
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：常驻线程，处理正常并发
        executor.setCorePoolSize(5);
        // 最大线程数：突发流量时扩展
        executor.setMaxPoolSize(20);
        // 队列容量：超过核心线程数时排队等待
        executor.setQueueCapacity(50);
        // 线程名前缀，便于日志定位
        executor.setThreadNamePrefix("coze-async-");
        // 关闭时等待任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
