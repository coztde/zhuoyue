package com.zhuoyue.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 项目启动入口。
 * 当前版本只保留真实在线的公开展示、课程管理和管理员登录能力，
 * 因此这里不再开启调度和 Git 平台属性绑定，只保留最基础的启动配置。
 */
@SpringBootApplication
@MapperScan("com.zhuoyue.platform.mapper")
public class ZhuoyuePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhuoyuePlatformApplication.class, args);
    }
}
