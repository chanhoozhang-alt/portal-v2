package com.example.portal.common.cache;

/**
 * Redis 缓存键前缀和默认过期时间常量。
 * 三个命名空间分别用于 Token 映射、用户身份和可见应用列表。
 */
public class CacheConstants {

    public static final String TOKEN_PREFIX = "portal:token:";
    public static final String IDENTITY_PREFIX = "portal:identity:";
    public static final String VISIBLE_APPS_PREFIX = "portal:visible:apps:";
    public static final long DEFAULT_TTL_SECONDS = 1800;
}
