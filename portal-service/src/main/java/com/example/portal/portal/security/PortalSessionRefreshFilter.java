package com.example.portal.portal.security;

import com.example.portal.common.cache.CacheConstants;
import com.example.portal.common.cache.PermissionCacheManager;
import com.example.portal.common.model.dto.auth.PortalSession;
import com.example.portal.common.util.JsonUtils;
import com.example.portal.portal.service.UpstreamAuthService;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** 在认证拦截器前尝试刷新即将过期的上游 accessToken。 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class PortalSessionRefreshFilter extends OncePerRequestFilter {

    private final PermissionCacheManager cacheManager;
    private final UpstreamAuthService upstreamAuthService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String sessionId = extractSessionId(request);
        if (StrUtil.isNotBlank(sessionId)) {
            String sessionJson = cacheManager.getSession(sessionId);
            PortalSession session = sessionJson != null
                    ? JsonUtils.toBean(sessionJson, PortalSession.class, true) : null;
            if (upstreamAuthService.shouldRefresh(session)) {
                try {
                    upstreamAuthService.refreshSession(sessionId, session, response);
                } catch (Exception e) {
                    log.warn("刷新上游accessToken失败，清理本系统会话: {}", e.getMessage());
                    upstreamAuthService.clearSession(sessionId, response);
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractSessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (CacheConstants.SESSION_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
