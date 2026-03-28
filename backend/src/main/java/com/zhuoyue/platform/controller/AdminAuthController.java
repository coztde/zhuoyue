package com.zhuoyue.platform.controller;

import com.zhuoyue.platform.common.result.Result;
import com.zhuoyue.platform.dto.AdminLoginRequest;
import com.zhuoyue.platform.service.AdminAuthService;
import com.zhuoyue.platform.vo.AdminLoginResponseVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员认证接口。
 */
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public Result<AdminLoginResponseVO> login(@RequestBody AdminLoginRequest request) {
        return Result.success(adminAuthService.login(request));
    }
}
