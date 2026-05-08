package com.example.portal.common.security;

import com.example.portal.common.context.UserContext;
import com.example.portal.common.exception.ForbiddenException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 静态权限校验工具类，封装基于 UserContext 的角色鉴权逻辑。
 * 所有方法在校验失败时抛出 ForbiddenException。
 */
public class PermissionChecker {

    public static void requireSystemAdmin() {
        UserContext ctx = UserContext.get();
        if (ctx == null || !ctx.isSystemAdmin()) {
            throw new ForbiddenException("需要系统管理员权限");
        }
    }

    public static void requireAppAdmin(String appCode) {
        UserContext ctx = UserContext.get();
        if (ctx == null) {
            throw new ForbiddenException("未认证");
        }
        if (!ctx.isAppAdmin(appCode)) {
            throw new ForbiddenException("无权操作该应用");
        }
    }

    public static void requireBizAdmin(String appCode) {
        UserContext ctx = UserContext.get();
        if (ctx == null) {
            throw new ForbiddenException("未认证");
        }
        if (!ctx.isBizAdmin(appCode)) {
            throw new ForbiddenException("无权操作该应用");
        }
    }

    /**
     * 按当前用户的数据权限过滤应用列表。
     * 系统管理员返回全部，非管理员只返回其在 appAdmin 或 bizAdmin 中有权限的 appCode。
     */
    public static List<String> filterAppCodesByScope(List<String> allAppCodes) {
        UserContext ctx = UserContext.get();
        if (ctx == null) {
            throw new ForbiddenException("未认证");
        }
        if (ctx.isSystemAdmin()) {
            return allAppCodes;
        }
        Set<String> userAppCodes = new HashSet<>();
        if (ctx.getAppAdminApps() != null) {
            ctx.getAppAdminApps().forEach(a -> userAppCodes.add(a.getAppCode()));
        }
        if (ctx.getBizAdminApps() != null) {
            ctx.getBizAdminApps().forEach(a -> userAppCodes.add(a.getAppCode()));
        }
        if (userAppCodes.isEmpty()) {
            throw new ForbiddenException("无权查看应用列表");
        }
        return allAppCodes.stream().filter(userAppCodes::contains).collect(Collectors.toList());
    }

    /**
     * 判断当前用户是否具有管理员身份（系统/应用/业务任一）。
     * 简化外部调用，避免直接依赖 UserContext。
     */
    public static boolean isAdmin() {
        UserContext ctx = UserContext.get();
        return ctx != null && ctx.isAdmin();
    }
}
