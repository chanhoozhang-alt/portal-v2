package com.example.portal.portal.controller;

import com.example.portal.common.model.common.Result;
import com.example.portal.common.model.dto.portal.PortalInitResponse;
import com.example.portal.portal.service.PortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 门户前端接口，提供首页初始化、应用列表、跳转信息等。
 */
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalController {

    private final PortalService portalService;

    /**
     * 门户首页初始化：返回用户信息、管理员标识和分组后的可见应用列表。
     */
    @GetMapping("/init")
    public Result<PortalInitResponse> init() {
        return Result.success(portalService.init());
    }

    /**
     * 获取可见应用列表（仅应用，不含用户信息）。
     */
    @GetMapping("/apps")
    public Result<PortalInitResponse> apps() {
        return Result.success(portalService.apps());
    }

    /**
     * 获取当前用户的管理员权限信息（不含应用列表）。
     */
    @GetMapping("/admin-permission")
    public Result<PortalInitResponse> adminPermission() {
        return Result.success(portalService.adminPermission());
    }

    /**
     * 获取指定应用的跳转信息，同时校验用户是否有权访问。
     */
    @GetMapping("/apps/{appCode}/jump-info")
    public Result<PortalInitResponse.AppItem> jumpInfo(@PathVariable String appCode) {
        return Result.success(portalService.jumpInfo(appCode));
    }
}
