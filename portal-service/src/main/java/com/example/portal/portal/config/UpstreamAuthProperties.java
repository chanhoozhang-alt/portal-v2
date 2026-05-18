package com.example.portal.portal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 上游统一认证平台配置。 */
@Data
@Component
@ConfigurationProperties(prefix = "portal.upstream-auth")
public class UpstreamAuthProperties {

    /** 上游认证登录地址。 */
    private String authorizeUrl = "https://one-account-gateway.paasuat.cmbchina.cn/auth-server/auth";

    /** 上游 token 接口地址。 */
    private String tokenUrl = "https://one-account-gateway.paasuat.cmbchina.cn/auth-server/token";

    /** 上游分配给本系统的客户端 ID。 */
    private String clientId;

    /** 上游分配给本系统的客户端密钥。 */
    private String clientSecret;

    /** 本系统 callback 地址，需在上游注册白名单。 */
    private String redirectUri = "http://localhost:8081/api/auth/callback";

    /** 授权范围。 */
    private String scope = "default";

    /** state TTL，单位秒。 */
    private long stateTtlSeconds = 600;

    /** accessToken 剩余多少秒以内触发 refreshToken 续期。 */
    private long refreshBeforeSeconds = 600;

    /** 本系统会话 Cookie 是否只允许 HTTPS 传输。生产环境建议设为 true。 */
    private boolean cookieSecure = false;

    /** 登录成功后默认跳转地址。 */
    private String defaultRedirect = "/";
}
