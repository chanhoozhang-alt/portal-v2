package com.example.portal.common.cache;

import com.example.portal.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 权限缓存管理器，封装三个缓存命名空间的读写操作：
 * <ul>
 *   <li>TOKEN_PREFIX —— Token 到 userId 的映射，用于拦截器链路</li>
 *   <li>IDENTITY_PREFIX —— 用户身份信息 JSON，用于恢复上下文</li>
 *   <li>VISIBLE_APPS_PREFIX —— 可见应用列表 JSON，供门户首页展示</li>
 * </ul>
 * 所有缓存使用统一的 DEFAULT_TTL_SECONDS，通过 renewTTL 延续。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionCacheManager {

    private final StringRedisTemplate redisTemplate;

    /** Token → userId 映射写入，用于拦截器快速校验身份。 */
    public void setToken(String token, String userId) {
        redisTemplate.opsForValue().set(
                CacheConstants.TOKEN_PREFIX + token, userId,
                CacheConstants.DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /** 根据 Token 获取 userId，未命中返回 null。 */
    public String getUserIdByToken(String token) {
        return redisTemplate.opsForValue().get(CacheConstants.TOKEN_PREFIX + token);
    }

    /** 写入用户身份 JSON，包含角色和管理员信息。 */
    public void setIdentity(String userId, Object identity) {
        redisTemplate.opsForValue().set(
                CacheConstants.IDENTITY_PREFIX + userId, JsonUtils.toJson(identity),
                CacheConstants.DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /** 获取用户身份 JSON，拦截器用其恢复请求上下文。 */
    public String getIdentity(String userId) {
        return redisTemplate.opsForValue().get(CacheConstants.IDENTITY_PREFIX + userId);
    }

    /** 写入用户可见应用列表 JSON 供门户首页使用。 */
    public void setVisibleApps(String userId, Object apps) {
        redisTemplate.opsForValue().set(
                CacheConstants.VISIBLE_APPS_PREFIX + userId, JsonUtils.toJson(apps),
                CacheConstants.DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /** 获取用户可见应用列表 JSON。 */
    public String getVisibleApps(String userId) {
        return redisTemplate.opsForValue().get(CacheConstants.VISIBLE_APPS_PREFIX + userId);
    }

    /** 清除指定用户的身份和可见应用缓存（身份变更时调用）。 */
    public void evictUser(String userId) {
        redisTemplate.unlink(Arrays.asList(
                CacheConstants.IDENTITY_PREFIX + userId,
                CacheConstants.VISIBLE_APPS_PREFIX + userId
        ));
    }

    /** 批量清除多个用户的缓存，使用 Pipeline 减少网络往返。 */
    public void evictUsers(List<String> userIds) {
        redisTemplate.executePipelined((RedisCallback<Void>) (connection) -> {
            for (String userId : userIds) {
                connection.unlink(
                        (CacheConstants.IDENTITY_PREFIX + userId).getBytes(),
                        (CacheConstants.VISIBLE_APPS_PREFIX + userId).getBytes()
                );
            }
            return null;
        });
    }

    /**
     * 清除所有用户的可见应用缓存。在应用增/删/改后调用。
     * 使用 SCAN 迭代替代 KEYS 避免阻塞，UNLINK 异步删除释放内存。
     */
    public void evictAllVisibleApps() {
        Set<String> keys = new HashSet<>();
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions()
                        .match(CacheConstants.VISIBLE_APPS_PREFIX + "*")
                        .count(100).build())) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        if (!keys.isEmpty()) {
            redisTemplate.unlink(keys);
        }
    }

    /**
     * 延长用户三个缓存键的 TTL（Token 映射、身份信息、可见应用），保持三者过期时间一致。
     * 在每次请求命中缓存时调用。
     */
    public void renewTTL(String token, String userId) {
        redisTemplate.expire(CacheConstants.TOKEN_PREFIX + token,
                CacheConstants.DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
        redisTemplate.expire(CacheConstants.IDENTITY_PREFIX + userId,
                CacheConstants.DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
        redisTemplate.expire(CacheConstants.VISIBLE_APPS_PREFIX + userId,
                CacheConstants.DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
    }
}
