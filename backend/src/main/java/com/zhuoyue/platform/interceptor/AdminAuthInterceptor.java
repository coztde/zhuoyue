package com.zhuoyue.platform.interceptor;

import com.zhuoyue.platform.common.exception.BizException;
import com.zhuoyue.platform.context.AdminContext;
import com.zhuoyue.platform.entity.AdminUser;
import com.zhuoyue.platform.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 后台认证拦截器。
 * 一期不引入完整 Spring Security，而是用一个轻量拦截器完成管理员令牌校验，
 * 这样依赖更少，也更适合当前“只有一个后台角色”的场景。
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final AdminAuthService adminAuthService;

    public AdminAuthInterceptor(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            throw new BizException("ADMIN_UNAUTHORIZED", "后台接口需要先登录");
        }

        AdminUser adminUser = adminAuthService.validateToken(token);
        AdminContext.set(adminUser.getId(), adminUser.getUsername());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AdminContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return request.getHeader("X-Admin-Token");
    }
}
