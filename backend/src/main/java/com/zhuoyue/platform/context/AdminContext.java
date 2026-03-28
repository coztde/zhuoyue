package com.zhuoyue.platform.context;

/**
 * 管理员上下文。
 * 这里使用最轻量的 ThreadLocal 保存当前请求的管理员信息，
 * 这样后台业务层在需要记录“是谁触发了操作”时，不必层层手动传参。
 */
public final class AdminContext {

    private static final ThreadLocal<Long> ADMIN_ID_HOLDER = new ThreadLocal<>();

    private static final ThreadLocal<String> ADMIN_USERNAME_HOLDER = new ThreadLocal<>();

    private AdminContext() {
    }

    public static void set(Long adminId, String username) {
        ADMIN_ID_HOLDER.set(adminId);
        ADMIN_USERNAME_HOLDER.set(username);
    }

    public static Long getAdminId() {
        return ADMIN_ID_HOLDER.get();
    }

    public static String getUsername() {
        return ADMIN_USERNAME_HOLDER.get();
    }

    public static void clear() {
        ADMIN_ID_HOLDER.remove();
        ADMIN_USERNAME_HOLDER.remove();
    }
}
