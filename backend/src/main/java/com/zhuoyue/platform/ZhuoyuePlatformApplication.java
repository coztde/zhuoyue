package com.zhuoyue.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 项目启动入口。
 * @EnableScheduling 开启定时任务支持，用于每30分钟自动同步学生 Git commit 数据。
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.zhuoyue.platform.mapper")
public class ZhuoyuePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhuoyuePlatformApplication.class, args);
    }
}
