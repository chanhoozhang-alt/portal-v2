package com.example.portal.server.service;

import com.example.portal.common.model.dto.server.AuthInitResponse;

/**
 * 认证服务接口：验证 Token 并返回用户身份、角色和可见应用信息。
 */
public interface AuthService {

    AuthInitResponse init(String token, String idToken);
}
