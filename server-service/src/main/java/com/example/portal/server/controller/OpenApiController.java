package com.example.portal.server.controller;

import com.example.portal.common.model.common.Result;
import com.example.portal.common.model.dto.server.OpenAccessResponse;
import com.example.portal.common.model.dto.server.OpenRoleResponse;
import com.example.portal.server.service.OpenApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 三方开放 API，需提供 ClientId/ClientSecret 认证。
 */
@RestController
@RequestMapping("/api/open")
@RequiredArgsConstructor
public class OpenApiController {

    private final OpenApiService openApiService;

    /**
     * 查询用户在指定应用下的角色列表。
     */
    @GetMapping("/apps/{appCode}/users/{userId}/roles")
    public Result<OpenRoleResponse> queryUserRoles(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestHeader("X-Client-Secret") String clientSecret,
            @PathVariable String appCode,
            @PathVariable String userId) {
        return Result.success(openApiService.queryUserRoles(clientId, clientSecret, appCode, userId));
    }

    /**
     * 校验用户是否有权访问指定应用。
     */
    @GetMapping("/apps/{appCode}/users/{userId}/access")
    public Result<OpenAccessResponse> checkUserAccess(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestHeader("X-Client-Secret") String clientSecret,
            @PathVariable String appCode,
            @PathVariable String userId) {
        return Result.success(openApiService.checkUserAccess(clientId, clientSecret, appCode, userId));
    }
}
