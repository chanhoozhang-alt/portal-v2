package com.example.portal.console.controller;

import com.example.portal.common.model.common.PageResult;
import com.example.portal.common.model.common.Result;
import com.example.portal.common.model.dto.console.RoleUserRequest;
import com.example.portal.common.model.entity.UserRole;
import com.example.portal.common.model.enums.CommonConstant;
import com.example.portal.common.security.PermissionChecker;
import com.example.portal.console.aspect.OperationLog;
import com.example.portal.console.service.RoleUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色人员分配接口，需业务管理员权限。
 */
@RestController
@RequiredArgsConstructor
public class RoleUserController {

    private final RoleUserService roleUserService;

    /**
     * 分页查询指定角色下的人员列表。
     */
    @GetMapping("/api/admin/apps/{appCode}/custom-roles/users")
    public Result<PageResult<UserRole>> listUsers(
            @PathVariable String appCode,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PermissionChecker.requireBizAdmin(appCode);
        return Result.success(roleUserService.listUsers(appCode, roleCode, userName, userId, status, pageNum, pageSize));
    }

    /**
     * 为角色添加人员（批量，自动跳过已存在的记录）。
     */
    @PostMapping("/api/admin/apps/{appCode}/custom-roles/users")
    @OperationLog(module = "角色人员", type = "ADD", desc = "添加角色人员")
    public Result<?> addUsers(
            @PathVariable String appCode,
            @RequestBody RoleUserRequest request) {
        PermissionChecker.requireBizAdmin(appCode);
        roleUserService.addUsers(appCode, request.getRoleCode(), request.getUserIds());
        return Result.success();
    }

    /**
     * 启用/停用角色下指定人员（body 传 roleCode + userIds + status=ENABLED/DISABLED）。
     */
    @PutMapping("/api/admin/apps/{appCode}/custom-roles/users/status")
    @OperationLog(module = "角色人员", type = "STATUS", desc = "启用/停用角色人员")
    public Result<?> updateUserStatus(
            @PathVariable String appCode,
            @RequestBody RoleUserRequest request) {
        PermissionChecker.requireBizAdmin(appCode);
        if (CommonConstant.STATUS_ENABLED.equals(request.getStatus())) {
            roleUserService.enableUser(appCode, request.getRoleCode(), request.getUserIds().get(0));
        } else {
            roleUserService.disableUser(appCode, request.getRoleCode(), request.getUserIds().get(0));
        }
        return Result.success();
    }

    /**
     * 从角色中移除人员（支持批量，roleCode + userIds 通过请求体传入）。
     */
    @DeleteMapping("/api/admin/apps/{appCode}/custom-roles/users")
    @OperationLog(module = "角色人员", type = "DELETE", desc = "移除角色人员")
    public Result<?> removeUsers(
            @PathVariable String appCode,
            @RequestBody RoleUserRequest request) {
        PermissionChecker.requireBizAdmin(appCode);
        roleUserService.batchRemoveUsers(appCode, request.getRoleCode(), request.getUserIds());
        return Result.success();
    }

    /**
     * 查询当前业务管理员所管辖应用的自定义角色列表（以应用分组展示）。
     */
    @GetMapping("/api/admin/biz-manage/custom-roles")
    public Result<List<Map<String, Object>>> listBizManageRoles() {
        return Result.success(roleUserService.listBizManageRoles());
    }
}
