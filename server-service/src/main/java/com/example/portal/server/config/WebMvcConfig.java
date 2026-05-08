package com.example.portal.server.config;

import com.example.portal.common.security.InternalTokenFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 认证服务内部接口保护配置。
 *
 * 职责：注册 InternalTokenFilter Servlet Filter，保护所有 /internal/* 路径。
 * server-service 的 /internal/* 接口是给 portal-service、console-service 通过 Feign 内部调用的，
 * 不允许外部直接访问。InternalTokenFilter 会校验请求中携带的内部调用 Token，
 * 没有或校验不通过的请求直接返回 401。
 *
 * 使用 FilterRegistrationBean 注册（而非 WebMvcConfigurer 拦截器），
 * 因为这是 Servlet 级别的过滤，在 Spring MVC 路由匹配之前执行，适合做接口级的访问控制。
 */
@Configuration
public class WebMvcConfig {

    /**
     * 注册内部调用 Token 校验过滤器。
     *
     * @param filter InternalTokenFilter 实例，由 Spring 自动注入
     * @return FilterRegistrationBean 包装了过滤器的 Servlet Filter 注册对象
     */
    @Bean
    public FilterRegistrationBean<InternalTokenFilter> internalTokenFilterRegistration(InternalTokenFilter filter) {
        FilterRegistrationBean<InternalTokenFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);                        // 设置过滤器实例
        registration.addUrlPatterns("/internal/*");            // 只拦截 /internal/* 路径
        registration.setOrder(1);                              // 执行优先级，数值越小越先执行
        return registration;
    }
}
