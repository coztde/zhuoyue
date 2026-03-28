package com.zhuoyue.platform.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员登录响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginResponseVO {

    private String token;

    private String username;

    private String displayName;

    private Long expireSeconds;
}
