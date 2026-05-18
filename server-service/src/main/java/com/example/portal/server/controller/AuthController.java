package com.example.portal.server.controller;

import com.example.portal.common.model.common.Result;
import com.example.portal.common.model.dto.server.AuthInitResponse;
import com.example.portal.server.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 内部认证接口，供 console-service 和 portal-service 通过 Feign 调用。
 */
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Token 认证初始化：验证 Token 并返回用户身份、角色和可见应用。
     */
    @PostMapping("/internal/auth/init")
    public Result<AuthInitResponse> init(@RequestHeader("X-User-Token") String token,
                                         @RequestHeader("X-ID-Token") String idToken) {
        return Result.success(authService.init(token, idToken));
    }
}
