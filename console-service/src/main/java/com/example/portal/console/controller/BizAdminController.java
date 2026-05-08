package com.example.portal.console.controller;

import com.example.portal.common.model.common.PageResult;
import com.example.portal.common.model.common.Result;
import com.example.portal.common.model.dto.console.UserIdRequest;
import com.example.portal.common.model.entity.BizAdmin;
import com.example.portal.common.model.enums.CommonConstant;
import com.example.portal.common.security.PermissionChecker;
import com.example.portal.console.aspect.OperationLog;
import com.example.portal.console.service.BizAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 业务管理员管理接口，需应用管理员权限。
 */
@RestController
@RequestMapping("/api/admin/apps/{appCode}/biz-admins")
@RequiredArgsConstructor
public class BizAdminController {

    private final BizAdminService bizAdminService;

    /**
     * 分页查询指定应用的业务管理员列表。
     */
    @GetMapping
    public Result<PageResult<BizAdmin>> list(
            @PathVariable String appCode,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PermissionChecker.requireAppAdmin(appCode);
        return Result.success(bizAdminService.list(appCode, pageNum, pageSize));
    }

    /**
     * 添加业务管理员（批量）。
     */
    @PostMapping
    @OperationLog(module = "业务管理员", type = "ADD", desc = "添加业务管理员")
    public Result<?> add(@PathVariable String appCode, @RequestBody UserIdRequest request) {
        PermissionChecker.requireAppAdmin(appCode);
        bizAdminService.add(appCode, request.getUserIds());
        return Result.success();
    }

    /**
     * 启用/停用业务管理员（body 传 userIds, query 传 status=ENABLED/DISABLED）。
     */
    @PutMapping("/status")
    @OperationLog(module = "业务管理员", type = "STATUS", desc = "启用/停用业务管理员")
    public Result<?> updateStatus(@PathVariable String appCode, @RequestBody UserIdRequest request,
                                   @RequestParam String status) {
        PermissionChecker.requireAppAdmin(appCode);
        if (CommonConstant.STATUS_ENABLED.equals(status)) {
            bizAdminService.enable(appCode, request.getUserIds().get(0));
        } else {
            bizAdminService.disable(appCode, request.getUserIds().get(0));
        }
        return Result.success();
    }

    /**
     * 移除指定业务管理员（支持批量，userId 列表通过请求体传入）。
     */
    @DeleteMapping
    @OperationLog(module = "业务管理员", type = "DELETE", desc = "移除业务管理员")
    public Result<?> remove(@PathVariable String appCode, @RequestBody UserIdRequest request) {
        PermissionChecker.requireAppAdmin(appCode);
        bizAdminService.removeBatch(appCode, request.getUserIds());
        return Result.success();
    }
}
