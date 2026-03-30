package com.zhuoyue.platform.config;

import com.zhuoyue.platform.interceptor.AdminAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 配置。
 * 当前只做一件事：保护后台接口，避免公开接口和后台接口混在一起没有边界。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;

    public WebMvcConfig(AdminAuthInterceptor adminAuthInterceptor) {
        this.adminAuthInterceptor = adminAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                // 登录接口本身不拦截
                .excludePathPatterns("/api/admin/auth/login");
        // /api/coze/** 为学生端公开接口，无需加入拦截器，此处仅作说明注释
    }
}
