package com.example.portal.portal.service;

import com.example.portal.common.cache.CacheConstants;
import com.example.portal.common.cache.PermissionCacheManager;
import com.example.portal.common.exception.BusinessException;
import com.example.portal.common.exception.UnauthorizedException;
import com.example.portal.common.model.common.Result;
import com.example.portal.common.model.dto.auth.AuthState;
import com.example.portal.common.model.dto.auth.PortalSession;
import com.example.portal.common.model.dto.server.AuthInitResponse;
import com.example.portal.common.util.JsonUtils;
import com.example.portal.portal.config.UpstreamAuthProperties;
import com.example.portal.portal.feign.ServerFeignClient;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/** 处理上游授权码登录、token 换取和本系统 session 建立。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpstreamAuthService {

    private final UpstreamAuthProperties properties;
    private final PermissionCacheManager cacheManager;
    private final ServerFeignClient serverFeignClient;

    @Value("${portal.internal-token:portal-internal-secret-2026}")
    private String internalToken;

    public String buildLoginUrl(String redirect) {
        validateLoginConfig();
        String state = IdUtil.fastSimpleUUID();
        AuthState authState = new AuthState();
        authState.setState(state);
        authState.setRedirectAfterLogin(safeRedirect(redirect));
        authState.setCreatedAt(System.currentTimeMillis());
        cacheManager.setAuthState(state, authState, properties.getStateTtlSeconds());

        return UriComponentsBuilder.fromHttpUrl(properties.getAuthorizeUrl())
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", properties.getScope())
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
    }

    public String handleCallback(String code, String state, HttpServletResponse response) {
        if (StrUtil.isBlank(code)) {
            throw new UnauthorizedException("缺少授权码code");
        }
        AuthState authState = consumeState(state);
        TokenResult token = exchangeTokenByCode(code);
        AuthInitResponse authInfo = initAuth(token);
        PortalSession session = createSession(authInfo.getUserId(), token);
        cacheManager.setSession(session.getSessionId(), session, token.getExpiresIn());
        addSessionCookie(response, session.getSessionId(), token.getExpiresIn());
        return authState.getRedirectAfterLogin();
    }

    public PortalSession refreshSession(String sessionId, PortalSession session, HttpServletResponse response) {
        TokenResult token = refreshToken(session.getRefreshToken());
        AuthInitResponse authInfo = initAuth(token);

        session.setUserId(authInfo.getUserId());
        session.setAccessToken(token.getAccessToken());
        session.setRefreshToken(StrUtil.blankToDefault(token.getRefreshToken(), session.getRefreshToken()));
        session.setIdToken(StrUtil.blankToDefault(token.getIdToken(), session.getIdToken()));
        session.setAccessTokenExpireAt(System.currentTimeMillis() + token.getExpiresIn() * 1000);
        session.setLastRefreshAt(System.currentTimeMillis());

        cacheManager.setSession(sessionId, session, token.getExpiresIn());
        addSessionCookie(response, sessionId, token.getExpiresIn());
        return session;
    }

    public boolean shouldRefresh(PortalSession session) {
        if (session == null || session.getAccessTokenExpireAt() == null) {
            return false;
        }
        long remainingMillis = session.getAccessTokenExpireAt() - System.currentTimeMillis();
        return remainingMillis <= properties.getRefreshBeforeSeconds() * 1000;
    }

    public void clearSession(String sessionId, HttpServletResponse response) {
        if (StrUtil.isNotBlank(sessionId)) {
            cacheManager.deleteSession(sessionId);
        }
        clearSessionCookie(response);
    }

    private AuthState consumeState(String state) {
        if (StrUtil.isBlank(state)) {
            throw new UnauthorizedException("缺少state");
        }
        String stateJson = cacheManager.getAuthState(state);
        if (stateJson == null) {
            throw new UnauthorizedException("登录已超时，请重新登录");
        }
        cacheManager.deleteAuthState(state);
        AuthState authState = JsonUtils.toBean(stateJson, AuthState.class, true);
        if (authState == null || !state.equals(authState.getState())) {
            throw new UnauthorizedException("state校验失败");
        }
        authState.setRedirectAfterLogin(safeRedirect(authState.getRedirectAfterLogin()));
        return authState;
    }

    private TokenResult exchangeTokenByCode(String code) {
        Map<String, Object> form = new HashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("code", code);
        form.put("client_id", properties.getClientId());
        form.put("client_secret", properties.getClientSecret());
        form.put("redirect_uri", properties.getRedirectUri());
        return requestToken(form);
    }

    private TokenResult refreshToken(String refreshToken) {
        if (StrUtil.isBlank(refreshToken)) {
            throw new UnauthorizedException("refreshToken不存在");
        }
        Map<String, Object> form = new HashMap<>();
        form.put("grant_type", "refresh_token");
        form.put("refresh_token", refreshToken);
        form.put("client_id", properties.getClientId());
        form.put("client_secret", properties.getClientSecret());
        return requestToken(form);
    }

    private TokenResult requestToken(Map<String, Object> form) {
        try {
            if (properties.isSignedTokenRequestEnabled()) {
                throw new BusinessException("上游 token 接口签名已启用，但当前未接入 ZA21 SignClient/TokenHelper 实现");
            }
            String body = HttpRequest.post(properties.getTokenUrl())
                    .form(form)
                    .timeout(properties.getReadTimeoutMillis())
                    .execute()
                    .body();
            JSONObject root = JSONUtil.parseObj(body);
            JSONObject data = root.getJSONObject("data");
            JSONObject tokenObj = data != null ? data : root;

            String accessToken = firstStr(tokenObj, "accessToken", "access_token", "token");
            if (StrUtil.isBlank(accessToken)) {
                log.warn("上游token接口未返回accessToken: {}", body);
                throw new UnauthorizedException("上游token接口返回无效");
            }

            TokenResult result = new TokenResult();
            result.setAccessToken(accessToken);
            result.setRefreshToken(firstStr(tokenObj, "refreshToken", "refresh_token"));
            result.setIdToken(firstStr(tokenObj, "idToken", "id_token"));
            if (StrUtil.isBlank(result.getIdToken())) {
                log.warn("上游token接口未返回id_token: {}", body);
                throw new UnauthorizedException("上游token接口返回缺少id_token");
            }
            result.setExpiresIn(firstLong(tokenObj, 10800L, "expiresIn", "expires_in"));
            return result;
        } catch (UnauthorizedException e) {
            throw e;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用上游token接口失败", e);
            throw new BusinessException("调用上游token接口失败");
        }
    }

    private AuthInitResponse initAuth(TokenResult token) {
        Result<AuthInitResponse> result = serverFeignClient.initAuth(internalToken, token.getAccessToken(), token.getIdToken());
        if (result == null || result.getCode() != 200 || result.getData() == null) {
            throw new UnauthorizedException("本系统认证初始化失败");
        }
        return result.getData();
    }

    private PortalSession createSession(String userId, TokenResult token) {
        long now = System.currentTimeMillis();
        PortalSession session = new PortalSession();
        session.setSessionId(IdUtil.fastSimpleUUID());
        session.setUserId(userId);
        session.setAccessToken(token.getAccessToken());
        session.setRefreshToken(token.getRefreshToken());
        session.setIdToken(token.getIdToken());
        session.setAccessTokenExpireAt(now + token.getExpiresIn() * 1000);
        session.setCreatedAt(now);
        session.setLastRefreshAt(now);
        return session;
    }

    private void addSessionCookie(HttpServletResponse response, String sessionId, long maxAgeSeconds) {
        StringBuilder cookie = new StringBuilder();
        cookie.append(CacheConstants.SESSION_COOKIE_NAME).append("=").append(sessionId)
                .append("; Path=/")
                .append("; Max-Age=").append(maxAgeSeconds)
                .append("; HttpOnly")
                .append("; SameSite=Lax");
        if (properties.isCookieSecure()) {
            cookie.append("; Secure");
        }
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearSessionCookie(HttpServletResponse response) {
        StringBuilder cookie = new StringBuilder();
        cookie.append(CacheConstants.SESSION_COOKIE_NAME).append("=")
                .append("; Path=/")
                .append("; Max-Age=0")
                .append("; HttpOnly")
                .append("; SameSite=Lax");
        if (properties.isCookieSecure()) {
            cookie.append("; Secure");
        }
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String safeRedirect(String redirect) {
        if (StrUtil.isBlank(redirect) || !redirect.startsWith("/") || redirect.startsWith("//")) {
            return properties.getDefaultRedirect();
        }
        return redirect;
    }

    private void validateLoginConfig() {
        if (StrUtil.isBlank(properties.getClientId())) {
            throw new BusinessException("缺少上游认证 clientId 配置");
        }
        if (StrUtil.isBlank(properties.getRedirectUri())) {
            throw new BusinessException("缺少上游认证 redirectUri 配置");
        }
        if (properties.getRedirectUri().contains("#")) {
            throw new BusinessException("上游认证 redirectUri 不能包含 #");
        }
    }

    private String firstStr(JSONObject obj, String... keys) {
        for (String key : keys) {
            String value = obj.getStr(key);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private long firstLong(JSONObject obj, long defaultValue, String... keys) {
        for (String key : keys) {
            Long value = obj.getLong(key);
            if (value != null && value > 0) {
                return value;
            }
        }
        return defaultValue;
    }

    @Data
    public static class TokenResult {
        private String accessToken;
        private String refreshToken;
        private String idToken;
        private long expiresIn;
    }
}
