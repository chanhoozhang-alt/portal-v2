package com.example.portal.portal.feign;

import com.example.portal.common.model.common.Result;
import com.example.portal.common.model.dto.server.AuthInitResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * server-service Feign 客户端，门户前端通过此接口完成 Token 回源认证。
 */
@FeignClient(name = "server-service")
public interface ServerFeignClient {

    @PostMapping("/internal/auth/init")
    Result<AuthInitResponse> initAuth(@RequestHeader("X-Internal-Token") String internalToken,
                                      @RequestHeader("X-User-Token") String token);
}
