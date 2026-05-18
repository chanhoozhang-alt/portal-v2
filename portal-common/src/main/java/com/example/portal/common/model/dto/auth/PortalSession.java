package com.example.portal.common.model.dto.auth;

import lombok.Data;

/** 本系统登录会话，保存在 Redis，浏览器 Cookie 中只保存随机 sessionId。 */
@Data
public class PortalSession {

    private String sessionId;
    private String userId;
    private String accessToken;
    private String refreshToken;
    private Long accessTokenExpireAt;
    private Long createdAt;
    private Long lastRefreshAt;
}
