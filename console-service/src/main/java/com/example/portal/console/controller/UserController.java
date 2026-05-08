package com.example.portal.console.controller;

import com.example.portal.common.model.common.PageResult;
import com.example.portal.common.model.common.Result;
import com.example.portal.common.security.PermissionChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 用户搜索接口，用于对接外部身份平台查询用户。
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {

    /**
     * 搜索用户（对接外部身份平台）。
     * 当前返回空列表，TODO 待对接身份平台后实现。
     */
    @GetMapping("/search")
    public Result<PageResult<Map<String, String>>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        // 校验管理员权限
        if (!PermissionChecker.isAdmin()) {
            return Result.error(403, "无权限");
        }
        // TODO: 对接外部身份平台查询人员
        // 当前返回空列表，待对接身份平台后替换
        return Result.success(new PageResult<>(0, Collections.emptyList()));
    }
}
