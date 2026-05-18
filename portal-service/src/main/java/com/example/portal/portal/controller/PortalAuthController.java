package com.example.portal.portal.controller;

import com.example.portal.common.cache.CacheConstants;
import com.example.portal.common.model.common.Result;
import com.example.portal.portal.service.UpstreamAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** 上游统一认证登录入口。 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PortalAuthController {

    private final UpstreamAuthService upstreamAuthService;

    /** 发起上游授权码登录。 */
    @GetMapping("/login")
    public void login(@RequestParam(value = "redirect", required = false) String redirect,
                      HttpServletResponse response) throws IOException {
        response.sendRedirect(upstreamAuthService.buildLoginUrl(redirect));
    }

    /** 上游认证成功后的 callback，接收 code，换 token，并建立本系统登录态。 */
    @GetMapping("/callback")
    public void callback(@RequestParam("code") String code,
                         @RequestParam("state") String state,
                         HttpServletResponse response) throws IOException {
        String redirect = upstreamAuthService.handleCallback(code, state, response);
        response.sendRedirect(redirect);
    }

    /** 退出本系统登录态。 */
    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request, HttpServletResponse response) {
        upstreamAuthService.clearSession(extractSessionId(request), response);
        return Result.success();
    }

    private String extractSessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (CacheConstants.SESSION_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
