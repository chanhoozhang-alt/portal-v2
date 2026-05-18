package com.example.portal.common.security;

import com.example.portal.common.cache.PermissionCacheManager;
import com.example.portal.common.cache.CacheConstants;
import com.example.portal.common.context.UserContext;
import com.example.portal.common.exception.UnauthorizedException;
import com.example.portal.common.model.common.AppBrief;
import com.example.portal.common.model.dto.auth.PortalSession;
import com.example.portal.common.util.JsonUtils;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;

/**
 * 认证拦截器，基于两级缓存策略验证用户身份：
 * <ol>
 *   <li>Token → userId 映射缓存，命中则直接恢复身份上下文</li>
 *   <li>用户身份信息缓存，未命中时通过 AuthFallbackHandler 回源调用认证服务</li>
 * </ol>
 * 每次命中后延长 TTL，请求结束后清理 ThreadLocal 上下文。
 */
@Slf4j
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final PermissionCacheManager cacheManager;
    private final AuthFallbackHandler fallbackHandler;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 查 token → userId 映射缓存；未命中则调用回源
        // 2. 查 userId → 身份信息缓存；未命中则调用回源
        // 两种缓存都不存在时才发起远程调用，命中任一层即可恢复上下文
        String token = extractToken(request);
        if (token == null || token.isEmpty()) {
            throw new UnauthorizedException("未提供认证Token");
        }

        String userId = cacheManager.getUserIdByToken(token);
        if (userId == null) {
            AuthInitResult result = fallbackHandler.initAuth(token);
            if (result == null) {
                throw new UnauthorizedException("Token验证失败");
            }
            userId = result.getUserId();
            fillContext(result);
            return true;
        }

        String identityJson = cacheManager.getIdentity(userId);
        if (identityJson == null) {
            AuthInitResult result = fallbackHandler.initAuth(token);
            if (result == null) {
                throw new UnauthorizedException("Token验证失败");
            }
            fillContext(result);
            return true;
        }

        fillContextFromJson(identityJson);
        cacheManager.renewTTL(token, userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    /**
     * 从请求中提取 Token，支持两种格式：
     * - Authorization: Bearer &lt;token&gt;
     * - Authorization: &lt;token&gt;（无 Bearer 前缀的兼容模式）
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        if (header != null && !header.isEmpty()) {
            return header;
        }
        return extractTokenFromSession(request);
    }

    /**
     * 从本系统 session Cookie 中恢复 accessToken。浏览器只保存随机 sessionId，
     * 真正的上游 token 保存在 Redis，避免暴露给前端页面。
     */
    private String extractTokenFromSession(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (CacheConstants.SESSION_COOKIE_NAME.equals(cookie.getName())) {
                String sessionJson = cacheManager.getSession(cookie.getValue());
                if (sessionJson == null) {
                    return null;
                }
                PortalSession session = JsonUtils.toBean(sessionJson, PortalSession.class, true);
                return session != null ? session.getAccessToken() : null;
            }
        }
        return null;
    }

    private void fillContext(AuthInitResult result) {
        UserContext ctx = new UserContext();
        ctx.setUserId(result.getUserId());
        ctx.setUserName(result.getUserName());
        ctx.setOrgId(result.getOrgId());
        ctx.setOrgName(result.getOrgName());
        ctx.setDeptId(result.getDeptId());
        ctx.setDeptName(result.getDeptName());
        ctx.setSystemAdmin(result.isSystemAdmin());
        ctx.setAppAdminApps(result.getAppAdminApps());
        ctx.setBizAdminApps(result.getBizAdminApps());
        ctx.setAdmin(ctx.isAdmin());
        UserContext.set(ctx);
    }

    /**
     * 从缓存的 JSON 字符串反序列化用户身份，避免远程调用。
     * 对应 AuthServiceImpl 中的 IdentityCache 结构。
     */
    private void fillContextFromJson(String json) {
        JSONObject obj = JsonUtils.toBean(json, JSONObject.class);
        UserContext ctx = new UserContext();
        ctx.setUserId(obj.getStr("userId"));
        ctx.setUserName(obj.getStr("userName"));
        ctx.setOrgId(obj.getStr("orgId"));
        ctx.setOrgName(obj.getStr("orgName"));
        ctx.setDeptId(obj.getStr("deptId"));
        ctx.setDeptName(obj.getStr("deptName"));
        ctx.setSystemAdmin(obj.getBool("isSystemAdmin", false));
        ctx.setAppAdminApps(parseAppBriefList(obj.getJSONArray("appAdminApps")));
        ctx.setBizAdminApps(parseAppBriefList(obj.getJSONArray("bizAdminApps")));
        ctx.setAdmin(ctx.isAdmin());
        UserContext.set(ctx);
    }

    private List<AppBrief> parseAppBriefList(JSONArray arr) {
        if (arr == null) {
            return new ArrayList<>();
        }
        List<AppBrief> list = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JSONObject item = arr.getJSONObject(i);
            list.add(new AppBrief(item.getStr("appCode"), item.getStr("appName")));
        }
        return list;
    }

    public interface AuthFallbackHandler {
        AuthInitResult initAuth(String token);
    }

    @lombok.Data
    public static class AuthInitResult {
        private String userId;
        private String userName;
        private String orgId;
        private String orgName;
        private String deptId;
        private String deptName;
        private boolean isSystemAdmin;
        private List<AppBrief> appAdminApps;
        private List<AppBrief> bizAdminApps;
    }
}
