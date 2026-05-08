package com.example.portal.console.controller;

import com.example.portal.common.model.common.Result;
import com.example.portal.common.model.dto.common.StatusRequest;
import com.example.portal.common.model.dto.console.AppGroupVO;
import com.example.portal.common.model.dto.console.GroupAppRequest;
import com.example.portal.common.model.enums.CommonConstant;
import com.example.portal.common.model.dto.console.GroupSaveRequest;
import com.example.portal.common.model.dto.console.GroupSortRequest;
import com.example.portal.common.security.PermissionChecker;
import com.example.portal.console.aspect.OperationLog;
import com.example.portal.console.service.AppGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应用分组管理接口，需系统管理员权限。
 */
@RestController
@RequestMapping("/api/admin/app-groups")
@RequiredArgsConstructor
public class AppGroupController {

    private final AppGroupService appGroupService;

    /**
     * 查询所有应用分组列表。
     */
    @GetMapping
    public Result<List<AppGroupVO>> list() {
        PermissionChecker.requireSystemAdmin();
        return Result.success(appGroupService.list());
    }

    /**
     * 新增应用分组。
     */
    @PostMapping
    @OperationLog(module = "应用分组", type = "ADD", desc = "新增应用分组")
    public Result<?> add(@RequestBody GroupSaveRequest request) {
        PermissionChecker.requireSystemAdmin();
        appGroupService.add(request);
        return Result.success();
    }

    /**
     * 修改应用分组信息。
     */
    @PutMapping("/{groupCode}")
    @OperationLog(module = "应用分组", type = "UPDATE", desc = "修改应用分组")
    public Result<?> update(@PathVariable String groupCode, @RequestBody GroupSaveRequest request) {
        PermissionChecker.requireSystemAdmin();
        appGroupService.update(groupCode, request);
        return Result.success();
    }

    /**
     * 启用/停用应用分组（status: ENABLED / DISABLED）。
     */
    @PutMapping("/{groupCode}/status")
    @OperationLog(module = "应用分组", type = "STATUS", desc = "启用/停用应用分组")
    public Result<?> updateStatus(@PathVariable String groupCode, @RequestBody StatusRequest request) {
        PermissionChecker.requireSystemAdmin();
        if (CommonConstant.STATUS_ENABLED.equals(request.getStatus())) {
            appGroupService.enable(groupCode);
        } else {
            appGroupService.disable(groupCode);
        }
        return Result.success();
    }

    /**
     * 绑定应用到指定分组。
     */
    @PostMapping("/{groupCode}/apps")
    @OperationLog(module = "应用分组", type = "BIND", desc = "绑定应用到分组")
    public Result<?> bindApps(@PathVariable String groupCode, @RequestBody GroupAppRequest request) {
        PermissionChecker.requireSystemAdmin();
        appGroupService.bindApps(groupCode, request.getAppCodes());
        return Result.success();
    }

    /**
     * 从分组中移除指定应用（appCode 通过查询参数传入）。
     */
    @DeleteMapping("/{groupCode}/apps")
    @OperationLog(module = "应用分组", type = "UNBIND", desc = "从分组移除应用")
    public Result<?> unbindApp(@PathVariable String groupCode, @RequestParam String appCode) {
        PermissionChecker.requireSystemAdmin();
        appGroupService.unbindApp(groupCode, appCode);
        return Result.success();
    }

    /**
     * 调整分组内应用的排序。
     */
    @PutMapping("/{groupCode}/apps/sort")
    @OperationLog(module = "应用分组", type = "SORT", desc = "调整分组内应用排序")
    public Result<?> sortApps(@PathVariable String groupCode, @RequestBody GroupSortRequest request) {
        PermissionChecker.requireSystemAdmin();
        appGroupService.sortApps(groupCode, request);
        return Result.success();
    }
}
