package com.zhuoyue.platform.service;

import com.zhuoyue.platform.dto.AdminLoginRequest;
import com.zhuoyue.platform.entity.AdminUser;
import com.zhuoyue.platform.vo.AdminLoginResponseVO;

/**
 * 管理员认证服务。
 */
public interface AdminAuthService {

    AdminLoginResponseVO login(AdminLoginRequest request);

    AdminUser validateToken(String token);
}
