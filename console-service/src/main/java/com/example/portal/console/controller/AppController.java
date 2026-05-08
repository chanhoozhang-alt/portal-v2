package com.example.portal.console.controller;

import com.example.portal.common.model.common.PageResult;
import com.example.portal.common.model.common.Result;
import com.example.portal.common.model.dto.common.StatusRequest;
import com.example.portal.common.model.dto.console.AppSaveRequest;
import com.example.portal.common.model.dto.console.AppVO;
import com.example.portal.common.model.enums.CommonConstant;
import com.example.portal.common.model.entity.AppInfo;
import com.example.portal.common.security.PermissionChecker;
import com.example.portal.console.aspect.OperationLog;
import com.example.portal.console.service.AppService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 应用管理接口，提供应用的增删改查、启用/停用和密钥重置功能。
 */
@RestController
@RequestMapping("/api/admin/apps")
@RequiredArgsConstructor
public class AppController {

    private final AppService appService;

    /**
     * 分页查询应用列表，非系统管理员按数据范围过滤。
     */
    @GetMapping
    public Result<PageResult<AppVO>> list(
            @RequestParam(required = false) String appCode,
            @RequestParam(required = false) String appName,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<AppInfo> result = appService.list(appCode, appName, status, pageNum, pageSize);
        List<AppVO> voList = result.getList().stream().map(AppVO::from).collect(Collectors.toList());
        return Result.success(new PageResult<>(result.getTotal(), voList));
    }

    /**
     * 根据 appCode 查询应用详情。
     */
    @GetMapping("/{appCode}")
    public Result<AppVO> get(@PathVariable String appCode) {
        PermissionChecker.requireAppAdmin(appCode);
        return Result.success(AppVO.from(appService.getByAppCode(appCode)));
    }

    @PostMapping("/save")
    @OperationLog(module = "应用管理", type = "SAVE", desc = "新增/修改应用")
    public Result<?> save(@RequestBody AppSaveRequest request) {
        // 新增应用需要系统管理员权限，修改应用需要该应用的应用管理员权限
        if (request.getAppCode() == null || request.getAppCode().isEmpty()) {
            PermissionChecker.requireSystemAdmin();
        } else {
            PermissionChecker.requireAppAdmin(request.getAppCode());
        }
        appService.save(request);
        return Result.success();
    }

    /**
     * 启用/停用指定应用（status: ENABLED / DISABLED）。
     */
    @PutMapping("/{appCode}/status")
    @OperationLog(module = "应用管理", type = "STATUS", desc = "启用/停用应用")
    public Result<?> updateStatus(@PathVariable String appCode, @RequestBody StatusRequest request) {
        PermissionChecker.requireAppAdmin(appCode);
        if (CommonConstant.STATUS_ENABLED.equals(request.getStatus())) {
            appService.enable(appCode);
        } else {
            appService.disable(appCode);
        }
        return Result.success();
    }

    /**
     * 重置应用的 ClientSecret（需系统管理员权限）。
     */
    @PostMapping("/{appCode}/client-secret/reset")
    @OperationLog(module = "应用管理", type = "RESET_SECRET", desc = "重置ClientSecret")
    public Result<Map<String, String>> resetClientSecret(@PathVariable String appCode) {
        PermissionChecker.requireSystemAdmin();
        String newSecret = appService.resetClientSecret(appCode);
        return Result.success(java.util.Collections.singletonMap("clientSecret", newSecret));
    }
}
