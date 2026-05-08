package com.example.portal.common.security;

import com.example.portal.common.exception.ForbiddenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 内部接口安全过滤器，保护所有 /internal/** 路径的请求。
 * 请求必须携带 X-Internal-Token 请求头，值需与服务端配置的 portal.internal-token 一致。
 * 优先级最高，在身份认证拦截器之前执行。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InternalTokenFilter extends OncePerRequestFilter {

    @Value("${portal.internal-token:portal-internal-secret-2026}")
    private String internalToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        // /internal/** 路径必须携带正确的 X-Internal-Token，其他路径放行
        if (path.startsWith("/internal")) {
            String token = request.getHeader("X-Internal-Token");
            if (token == null || !token.equals(internalToken)) {
                throw new ForbiddenException("内部接口认证失败");
            }
        }
        filterChain.doFilter(request, response);
    }
}
