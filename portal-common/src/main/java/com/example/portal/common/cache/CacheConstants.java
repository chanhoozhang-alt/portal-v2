package com.example.portal.common.cache;

/**
 * Redis 缓存键前缀和默认过期时间常量。
 * 三个命名空间分别用于 Token 映射、用户身份和可见应用列表。
 */
public class CacheConstants {

    public static final String TOKEN_PREFIX = "portal:token:";
    public static final String IDENTITY_PREFIX = "portal:identity:";
    public static final String VISIBLE_APPS_PREFIX = "portal:visible:apps:";
    public static final String SESSION_PREFIX = "portal:session:";
    public static final String AUTH_STATE_PREFIX = "portal:auth:state:";
    public static final String SESSION_COOKIE_NAME = "PORTAL_SESSION";
    public static final long DEFAULT_TTL_SECONDS = 1800;
    public static final long AUTH_STATE_TTL_SECONDS = 600;
}
