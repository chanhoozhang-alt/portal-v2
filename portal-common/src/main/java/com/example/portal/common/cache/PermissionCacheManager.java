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

    /** Token → userId 映射写入，TTL 与上游 accessToken 剩余有效期保持一致。 */
    public void setToken(String token, String userId, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                CacheConstants.TOKEN_PREFIX + token, userId,
                ttlSeconds, TimeUnit.SECONDS);
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

    /** 写入用户身份 JSON，TTL 与当前会话/token 有效期保持一致。 */
    public void setIdentity(String userId, Object identity, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                CacheConstants.IDENTITY_PREFIX + userId, JsonUtils.toJson(identity),
                ttlSeconds, TimeUnit.SECONDS);
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

    /** 写入用户可见应用列表 JSON，TTL 与当前会话/token 有效期保持一致。 */
    public void setVisibleApps(String userId, Object apps, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                CacheConstants.VISIBLE_APPS_PREFIX + userId, JsonUtils.toJson(apps),
                ttlSeconds, TimeUnit.SECONDS);
    }

    /** 获取用户可见应用列表 JSON。 */
    public String getVisibleApps(String userId) {
        return redisTemplate.opsForValue().get(CacheConstants.VISIBLE_APPS_PREFIX + userId);
    }

    /** 写入本系统登录会话，浏览器 Cookie 中只保存随机 sessionId。 */
    public void setSession(String sessionId, Object session, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                CacheConstants.SESSION_PREFIX + sessionId, JsonUtils.toJson(session),
                ttlSeconds, TimeUnit.SECONDS);
    }

    /** 获取本系统登录会话 JSON。 */
    public String getSession(String sessionId) {
        return redisTemplate.opsForValue().get(CacheConstants.SESSION_PREFIX + sessionId);
    }

    /** 删除本系统登录会话。 */
    public void deleteSession(String sessionId) {
        redisTemplate.unlink(CacheConstants.SESSION_PREFIX + sessionId);
    }

    /** 写入登录 state，短 TTL，一次登录流程使用一次。 */
    public void setAuthState(String state, Object value, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                CacheConstants.AUTH_STATE_PREFIX + state, JsonUtils.toJson(value),
                ttlSeconds, TimeUnit.SECONDS);
    }

    /** 读取登录 state。 */
    public String getAuthState(String state) {
        return redisTemplate.opsForValue().get(CacheConstants.AUTH_STATE_PREFIX + state);
    }

    /** 删除登录 state，避免重复使用。 */
    public void deleteAuthState(String state) {
        redisTemplate.unlink(CacheConstants.AUTH_STATE_PREFIX + state);
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

    /** 按指定 TTL 续期 Token、身份和可见应用缓存，不超过上游 token 剩余有效期。 */
    public void renewTTL(String token, String userId, long ttlSeconds) {
        redisTemplate.expire(CacheConstants.TOKEN_PREFIX + token, ttlSeconds, TimeUnit.SECONDS);
        redisTemplate.expire(CacheConstants.IDENTITY_PREFIX + userId, ttlSeconds, TimeUnit.SECONDS);
        redisTemplate.expire(CacheConstants.VISIBLE_APPS_PREFIX + userId, ttlSeconds, TimeUnit.SECONDS);
    }
}
