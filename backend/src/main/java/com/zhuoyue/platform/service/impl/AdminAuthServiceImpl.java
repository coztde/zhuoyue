package com.zhuoyue.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhuoyue.platform.common.constant.CacheConstants;
import com.zhuoyue.platform.common.exception.BizException;
import com.zhuoyue.platform.dto.AdminLoginRequest;
import com.zhuoyue.platform.entity.AdminUser;
import com.zhuoyue.platform.mapper.AdminUserMapper;
import com.zhuoyue.platform.service.AdminAuthService;
import com.zhuoyue.platform.util.PasswordHashUtils;
import com.zhuoyue.platform.vo.AdminLoginResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 管理员认证服务实现。
 */
@Slf4j
@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final long SESSION_EXPIRE_SECONDS = 60 * 60 * 12L;

    private final AdminUserMapper adminUserMapper;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 本地会话兜底缓存。
     * Redis 配置错误或本地没开 Redis 时，后台登录不应该整条链路直接报废，
     * 单机开发环境先降级到内存会话，保证联调可以继续。
     */
    private final Map<String, SessionState> localSessionStore = new ConcurrentHashMap<>();

    /** 是否已经打过 Redis 不可用的 WARN 日志，避免每次请求都刷屏 */
    private final AtomicBoolean redisWarnLogged = new AtomicBoolean(false);

    public AdminAuthServiceImpl(AdminUserMapper adminUserMapper,
                                StringRedisTemplate stringRedisTemplate) {
        this.adminUserMapper = adminUserMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public AdminLoginResponseVO login(AdminLoginRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new BizException("LOGIN_PARAM_INVALID", "用户名和密码不能为空");
        }

        AdminUser adminUser = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, request.getUsername())
                .last("limit 1"));
        if (adminUser == null || !"ACTIVE".equalsIgnoreCase(adminUser.getActiveStatus())) {
            throw new BizException("LOGIN_FAILED", "管理员账号不存在或已停用");
        }

        if (!PasswordHashUtils.matches(request.getPassword(), adminUser.getPasswordHash())) {
            throw new BizException("LOGIN_FAILED", "用户名或密码错误");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        String cacheValue = adminUser.getId() + ":" + adminUser.getUsername();
        cacheSession(token, cacheValue);

        return AdminLoginResponseVO.builder()
                .token(token)
                .username(adminUser.getUsername())
                .displayName(adminUser.getDisplayName())
                .expireSeconds(SESSION_EXPIRE_SECONDS)
                .build();
    }

    @Override
    public AdminUser validateToken(String token) {
        String cacheValue = readSession(token);
        if (!StringUtils.hasText(cacheValue) || !cacheValue.contains(":")) {
            throw new BizException("ADMIN_UNAUTHORIZED", "登录状态已失效，请重新登录");
        }

        String[] parts = cacheValue.split(":", 2);
        AdminUser adminUser = adminUserMapper.selectById(Long.parseLong(parts[0]));
        if (adminUser == null || !"ACTIVE".equalsIgnoreCase(adminUser.getActiveStatus())) {
            throw new BizException("ADMIN_UNAUTHORIZED", "管理员账号已失效");
        }
        refreshSession(token, cacheValue);
        return adminUser;
    }

    private void cacheSession(String token, String cacheValue) {
        localSessionStore.put(token, new SessionState(cacheValue, LocalDateTime.now().plusSeconds(SESSION_EXPIRE_SECONDS)));
        try {
            stringRedisTemplate.opsForValue().set(
                    CacheConstants.ADMIN_SESSION_PREFIX + token,
                    cacheValue,
                    Duration.ofSeconds(SESSION_EXPIRE_SECONDS)
            );
        } catch (Exception exception) {
            if (redisWarnLogged.compareAndSet(false, true)) {
                log.warn("Redis 不可用，会话降级到本地内存缓存（后续相同警告将降为 DEBUG 级别）");
            } else {
                log.debug("Redis 不可用，会话写入本地内存缓存");
            }
        }
    }

    private String readSession(String token) {
        try {
            String redisValue = stringRedisTemplate.opsForValue().get(CacheConstants.ADMIN_SESSION_PREFIX + token);
            if (StringUtils.hasText(redisValue)) {
                return redisValue;
            }
        } catch (Exception exception) {
            // 首次失败打 WARN，后续只打 debug，避免每次请求都刷屏
            if (redisWarnLogged.compareAndSet(false, true)) {
                log.warn("Redis 不可用，会话降级到本地内存缓存（后续相同警告将降为 DEBUG 级别）");
            } else {
                log.debug("读取 Redis 会话失败，继续使用本地内存会话");
            }
        }

        SessionState localSession = localSessionStore.get(token);
        if (localSession == null) {
            return null;
        }
        if (localSession.expireAt().isBefore(LocalDateTime.now())) {
            localSessionStore.remove(token);
            return null;
        }
        return localSession.cacheValue();
    }

    private void refreshSession(String token, String cacheValue) {
        localSessionStore.put(token, new SessionState(cacheValue, LocalDateTime.now().plusSeconds(SESSION_EXPIRE_SECONDS)));
        try {
            stringRedisTemplate.expire(CacheConstants.ADMIN_SESSION_PREFIX + token, Duration.ofSeconds(SESSION_EXPIRE_SECONDS));
        } catch (Exception exception) {
            log.debug("刷新 Redis 会话过期时间失败，继续使用本地内存会话");
        }
    }

    private record SessionState(String cacheValue, LocalDateTime expireAt) {
    }
}
