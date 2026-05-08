package com.example.portal.console.config;

import com.example.portal.common.cache.PermissionCacheManager;
import com.example.portal.common.model.common.Result;
import com.example.portal.common.model.dto.server.AuthInitResponse;
import com.example.portal.common.security.AuthInterceptor;
import com.example.portal.console.feign.ServerFeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.handler.MappedInterceptor;

/**
 * 管理后台认证配置。
 *
 * 职责：将 AuthInterceptor 注册为 Spring MVC 拦截器，拦截所有 /api/admin/** 请求，
 * 从请求 Header 中提取 Token，通过 Feign 调用 server-service 验证并获取用户信息，
 * 存入 UserContext（ThreadLocal）供后续业务代码使用。
 *
 * 采用 @Bean 返回 MappedInterceptor 的方式注册，而非实现 WebMvcConfigurer 接口，
 * 因为 WebMvcConfigurer 会在 MVC 初始化早期被收集，此时 Feign 客户端还未就绪，会形成循环依赖。
 * MappedInterceptor 在 MVC 初始化完成后通过 Bean 后处理注册，避开了这个问题。
 */
@Configuration
public class WebMvcConfig {

    /**0
     * 注册管理后台认证拦截器。
     *
     * @param cacheManager       Redis 缓存管理器，用于缓存已验证的用户信息，避免每次请求都调远程接口
     * @param serverFeignClient  Feign 客户端，远程调用 server-service 的 initAuth 接口验证 Token
     * @return MappedInterceptor 绑定了拦截路径和拦截器实例的 Spring MVC 拦截器注册对象
     */
    @Bean
    public MappedInterceptor adminAuthInterceptor(PermissionCacheManager cacheManager,
                                                   ServerFeignClient serverFeignClient) {
        // 创建认证拦截器，构造参数 2 是 Token 验证策略（Lambda）：
        // AuthInterceptor 拿到请求中的 Token 后，不知道怎么验证，由这里告诉它具体的验证逻辑
        AuthInterceptor interceptor = new AuthInterceptor(cacheManager, token -> {

            // 拿着 Token 远程调用 server-service，获取该 Token 对应的用户信息
            Result<AuthInitResponse> result = serverFeignClient.initAuth(token);

            // 验证失败返回 null，AuthInterceptor 收到 null 会直接返回 401
            if (result == null || result.getCode() != 200 || result.getData() == null) {
                return null;
            }

            // 将 server-service 返回的 DTO 转换为 AuthInterceptor 内部的通用格式
            // 这样 AuthInterceptor 不需要依赖具体服务的 DTO 类型，保持解耦
            AuthInitResponse data = result.getData();
            AuthInterceptor.AuthInitResult r = new AuthInterceptor.AuthInitResult();
            r.setUserId(data.getUserId());
            r.setUserName(data.getUserName());
            r.setOrgId(data.getOrgId());
            r.setOrgName(data.getOrgName());
            r.setDeptId(data.getDeptId());
            r.setDeptName(data.getDeptName());
            r.setSystemAdmin(data.isSystemAdmin());
            r.setAppAdminApps(data.getAppAdminApps());
            r.setBizAdminApps(data.getBizAdminApps());
            return r;
        });

        // 将拦截器与路径绑定：所有匹配 /api/admin/** 的请求都会经过这个拦截器
        return new MappedInterceptor(new String[]{"/api/admin/**"}, null, interceptor);
    }
}
