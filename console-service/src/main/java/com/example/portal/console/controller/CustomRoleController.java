package com.example.portal.console.controller;

import com.example.portal.common.model.common.Result;
import com.example.portal.common.model.dto.console.CustomRoleRequest;
import com.example.portal.common.model.enums.CommonConstant;
import com.example.portal.common.model.entity.AppCustomRole;
import com.example.portal.common.security.PermissionChecker;
import com.example.portal.console.aspect.OperationLog;
import com.example.portal.console.service.CustomRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 自定义角色管理接口，需应用管理员权限。
 */
@RestController
@RequestMapping("/api/admin/apps/{appCode}/custom-roles")
@RequiredArgsConstructor
public class CustomRoleController {

    private final CustomRoleService customRoleService;

    /**
     * 查询指定应用下的自定义角色列表，支持按角色编码、名称和状态筛选。
     */
    @GetMapping
    public Result<List<AppCustomRole>> list(
            @PathVariable String appCode,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String status) {
        PermissionChecker.requireAppAdmin(appCode);
        return Result.success(customRoleService.list(appCode, roleCode, roleName, status));
    }

    /**
     * 新增自定义角色。
     */
    @PostMapping
    @OperationLog(module = "自定义角色", type = "ADD", desc = "新增自定义角色")
    public Result<?> add(@PathVariable String appCode, @RequestBody CustomRoleRequest request) {
        PermissionChecker.requireAppAdmin(appCode);
        customRoleService.add(appCode, request.getRoleCode(), request.getRoleName(), request.getRoleDesc());
        return Result.success();
    }

    /**
     * 修改自定义角色名称和描述（roleCode 通过请求体传入）。
     */
    @PutMapping
    @OperationLog(module = "自定义角色", type = "UPDATE", desc = "修改自定义角色")
    public Result<?> update(@PathVariable String appCode, @RequestBody CustomRoleRequest request) {
        PermissionChecker.requireAppAdmin(appCode);
        customRoleService.update(appCode, request.getRoleCode(), request.getRoleName(), request.getRoleDesc());
        return Result.success();
    }

    /**
     * 启用/停用自定义角色（body 传 roleCode + status=ENABLED/DISABLED）。
     */
    @PutMapping("/status")
    @OperationLog(module = "自定义角色", type = "STATUS", desc = "启用/停用自定义角色")
    public Result<?> updateStatus(@PathVariable String appCode, @RequestBody CustomRoleRequest request) {
        PermissionChecker.requireAppAdmin(appCode);
        if (CommonConstant.STATUS_ENABLED.equals(request.getStatus())) {
            customRoleService.enable(appCode, request.getRoleCode());
        } else {
            customRoleService.disable(appCode, request.getRoleCode());
        }
        return Result.success();
    }
}
