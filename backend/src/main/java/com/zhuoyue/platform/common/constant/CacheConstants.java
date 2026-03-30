package com.zhuoyue.platform.common.constant;

/**
 * 缓存 Key 常量。
 * 统一前缀的目的是避免后期多人协作时出现 Key 命名混乱，
 * 同时也方便 Redis 里按业务域排查问题。
 */
public final class CacheConstants {

    private CacheConstants() {
    }

    public static final String ADMIN_SESSION_PREFIX = "zhuoyue:admin:session:";

    public static final String WIKI_LIKES_PREFIX = "zhuoyue:wiki:likes:";

}
