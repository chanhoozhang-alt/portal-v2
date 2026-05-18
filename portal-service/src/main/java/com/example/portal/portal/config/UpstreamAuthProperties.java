package com.example.portal.portal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 上游统一认证平台配置。 */
@Data
@Component
@ConfigurationProperties(prefix = "portal.upstream-auth")
public class UpstreamAuthProperties {

    /** 上游认证登录地址，浏览器 302 跳转到这里获取授权码 code。 */
    private String authorizeUrl = "https://one-account-gateway.paasuat.cmbchina.cn/auth-server/auth";

    /** 上游 token 接口地址，后端用授权码或 refreshToken 换取 token；UAT 文档要求使用 http。 */
    private String tokenUrl = "http://one-account-gateway.paasuat.cmbchina.cn/auth-server/token";

    /** 上游认证中心根地址，使用 ZA21 工具包或 SignClient 时作为 centerUri。 */
    private String centerUri = "http://one-account-gateway.paasuat.cmbchina.cn/auth-server";

    /** 上游分配给本系统的客户端 ID。 */
    private String clientId;

    /** 上游分配给本系统的客户端密钥；正式签名模式下可能不再依赖该字段。 */
    private String clientSecret;

    /** 国密算法应用私钥，注册应用公钥对应的私钥，用于 token 接口请求签名。 */
    private String privateKey;

    /** 国密算法应用公钥，和 privateKey 配套，便于工具包或签名实现读取。 */
    private String publicKey;

    /** 认证中心国密公钥，用于验证 id_token 的 SM3WithSM2 签名。 */
    private String centerPublicKey;

    /** RSA 算法应用私钥，和 privateKey 二选一，用于非国密签名场景。 */
    private String rsaPrivateKey;

    /** RSA 算法应用公钥，和 rsaPrivateKey 配套。 */
    private String rsaPublicKey;

    /** 认证中心 RSA 512 位公钥，用于验证 id_token。 */
    private String centerRsaPublicKey;

    /** 认证中心 RSA 2048 位公钥，用于验证 id_token。 */
    private String centerRsa2048PublicKey;

    /** 本系统 callback 地址，需在上游注册白名单；不能包含 #。 */
    private String redirectUri = "http://localhost:8081/api/auth/callback";

    /** 授权范围，默认 default。 */
    private String scope = "default";

    /** 是否启用上游 token 接口签名；未接入 ZA21/签名实现前保持 false。 */
    private boolean signedTokenRequestEnabled = false;

    /** 是否强制对 id_token 做签名校验；公钥未配置完整前保持 false，只做 payload 解析。 */
    private boolean strictIdTokenVerify = false;

    /** 调上游 token 接口连接超时，单位毫秒。 */
    private int connectionTimeoutMillis = 10000;

    /** 调上游 token 接口读取超时，单位毫秒。 */
    private int readTimeoutMillis = 60000;

    /** state TTL，单位秒。 */
    private long stateTtlSeconds = 600;

    /** accessToken 剩余多少秒以内触发 refreshToken 续期。 */
    private long refreshBeforeSeconds = 600;

    /** 本系统会话 Cookie 是否只允许 HTTPS 传输；生产环境建议设为 true。 */
    private boolean cookieSecure = false;

    /** 登录成功后默认跳转地址。 */
    private String defaultRedirect = "/";
}
