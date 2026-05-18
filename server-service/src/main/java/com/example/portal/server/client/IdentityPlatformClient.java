package com.example.portal.server.client;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** 从上游 OIDC id_token 恢复用户身份。 */
@Slf4j
@Component
public class IdentityPlatformClient {

    @Value("${portal.upstream-auth.client-id:}")
    private String clientId;

    @Value("${portal.upstream-auth.strict-id-token-verify:false}")
    private boolean strictIdTokenVerify;

    @Value("${portal.upstream-auth.center-public-key:}")
    private String centerPublicKey;

    @Value("${portal.upstream-auth.center-rsa-public-key:}")
    private String centerRsaPublicKey;

    @Value("${portal.upstream-auth.center-rsa2048-public-key:}")
    private String centerRsa2048PublicKey;

    /**
     * 解析 id_token 获取用户身份。accessToken 仅作为本系统会话凭据缓存，不再用于回源获取用户信息。
     */
    public IdentityInfo verifyToken(String accessToken, String idToken) {
        if (StrUtil.isBlank(idToken)) {
            log.warn("认证初始化缺少id_token");
            return null;
        }
        return parseIdToken(idToken);
    }

    /**
     * 当前先完成 id_token payload 解析和 aud 校验。若开启 strict-id-token-verify，则必须接入上游文档要求的
     * SM2WithSM3/RS256 验签实现后才能放行，避免生产环境误用未验签 JWT。
     */
    private IdentityInfo parseIdToken(String idToken) {
        try {
            if (strictIdTokenVerify) {
                ensureVerifyKeysConfigured();
                throw new IllegalStateException("strict-id-token-verify 已开启，但尚未接入 SM2WithSM3/RS256 验签实现");
            }

            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                log.warn("id_token 格式非法");
                return null;
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JSONObject claims = JSONUtil.parseObj(payload);
            validateAudience(claims);

            IdentityInfo info = new IdentityInfo();
            info.setUserId(firstStr(claims, "openId", "userId", "sub"));
            info.setUserName(claims.getStr("userName"));
            info.setOrgId(claims.getStr("orgId"));
            info.setOrgName(claims.getStr("orgName"));
            info.setDeptId(firstStr(claims, "deptId", "originOrgId"));
            info.setDeptName(firstStr(claims, "deptName", "pathName"));
            return StrUtil.isBlank(info.getUserId()) ? null : info;
        } catch (Exception e) {
            log.warn("解析上游id_token失败: {}", e.getMessage());
            return null;
        }
    }

    private void ensureVerifyKeysConfigured() {
        if (StrUtil.isAllBlank(centerPublicKey, centerRsaPublicKey, centerRsa2048PublicKey)) {
            throw new IllegalStateException("缺少认证中心公钥配置，无法校验id_token签名");
        }
    }

    private void validateAudience(JSONObject claims) {
        if (StrUtil.isBlank(clientId)) {
            return;
        }
        Object aud = claims.get("aud");
        if (aud == null) {
            return;
        }
        String audText = String.valueOf(aud);
        if (!audText.contains(clientId)) {
            throw new IllegalStateException("id_token非本应用受众");
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

    @lombok.Data
    public static class IdentityInfo {
        private String userId;
        private String userName;
        private String orgId;
        private String orgName;
        private String deptId;
        private String deptName;
    }
}
