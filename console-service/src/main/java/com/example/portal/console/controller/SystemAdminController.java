package com.example.portal.console.controller;

import com.example.portal.common.model.common.PageResult;
import com.example.portal.common.model.common.Result;
import com.example.portal.common.model.dto.common.StatusRequest;
import com.example.portal.common.model.dto.console.UserIdRequest;
import com.example.portal.common.model.entity.SystemAdmin;
import com.example.portal.common.model.enums.CommonConstant;
import com.example.portal.common.security.PermissionChecker;
import com.example.portal.console.aspect.OperationLog;
import com.example.portal.console.service.SystemAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 系统管理员管理接口，需系统管理员权限。
 */
@RestController
@RequestMapping("/api/admin/system-admins")
@RequiredArgsConstructor
public class SystemAdminController {

    private final SystemAdminService systemAdminService;

    /**
     * 分页查询系统管理员列表。
     */
    @GetMapping
    public Result<PageResult<SystemAdmin>> list(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PermissionChecker.requireSystemAdmin();
        return Result.success(systemAdminService.list(userName, userId, pageNum, pageSize));
    }

    /**
     * 添加系统管理员（批量）。
     */
    @PostMapping
    @OperationLog(module = "系统管理员", type = "ADD", desc = "添加系统管理员")
    public Result<?> add(@RequestBody UserIdRequest request) {
        PermissionChecker.requireSystemAdmin();
        systemAdminService.add(request.getUserIds());
        return Result.success();
    }

    /**
     * 启用/停用指定系统管理员（status: ENABLED / DISABLED）。
     */
    @PutMapping("/{userId}/status")
    @OperationLog(module = "系统管理员", type = "STATUS", desc = "启用/停用系统管理员")
    public Result<?> updateStatus(@PathVariable String userId, @RequestBody StatusRequest request) {
        PermissionChecker.requireSystemAdmin();
        if (CommonConstant.STATUS_ENABLED.equals(request.getStatus())) {
            systemAdminService.enable(userId);
        } else {
            systemAdminService.disable(userId);
        }
        return Result.success();
    }

    /**
     * 移除指定系统管理员（从数据库删除记录）。
     */
    @DeleteMapping("/{userId}")
    @OperationLog(module = "系统管理员", type = "DELETE", desc = "移除系统管理员")
    public Result<?> remove(@PathVariable String userId) {
        PermissionChecker.requireSystemAdmin();
        systemAdminService.remove(userId);
        return Result.success();
    }
}
