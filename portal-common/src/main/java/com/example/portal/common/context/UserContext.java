package com.example.portal.common.context;

import com.example.portal.common.model.common.AppBrief;
import lombok.Data;

import java.util.List;

/**
 * 请求级用户身份上下文，基于 ThreadLocal 存储。
 * 请求结束后必须通过 clear() 清理（由 AuthInterceptor.afterCompletion 负责），
 * 避免线程复用导致的内存泄漏或身份串号。
 */
@Data
public class UserContext {

    private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

    private String userId;
    private String userName;
    private String orgId;
    private String orgName;
    private String deptId;
    private String deptName;
    /** 是否管理员(系统/应用/业务任一) */
    private boolean isAdmin;
    /** 是否系统管理员 */
    private boolean isSystemAdmin;
    /** 有应用管理员权限的应用列表 */
    private List<AppBrief> appAdminApps;
    /** 有业务管理员权限的应用列表 */
    private List<AppBrief> bizAdminApps;

    public static void set(UserContext ctx) {
        HOLDER.set(ctx);
    }

    public static UserContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 判断是否指定应用的应用管理员。系统管理员拥有所有应用的管理权限。
     */
    public boolean isAppAdmin(String appCode) {
        if (isSystemAdmin) {
            return true;
        }
        if (appAdminApps == null) {
            return false;
        }
        return appAdminApps.stream().anyMatch(a -> a.getAppCode().equals(appCode));
    }

    /**
     * 判断是否指定应用的业务管理员。系统管理员拥有所有应用的管理权限。
     */
    public boolean isBizAdmin(String appCode) {
        if (isSystemAdmin) {
            return true;
        }
        if (bizAdminApps == null) {
            return false;
        }
        return bizAdminApps.stream().anyMatch(a -> a.getAppCode().equals(appCode));
    }

    /**
     * 判断是否具有管理员身份（系统/应用/业务任一管理员均可返回 true）。
     */
    public boolean isAdmin() {
        return isSystemAdmin || (appAdminApps != null && !appAdminApps.isEmpty())
                || (bizAdminApps != null && !bizAdminApps.isEmpty());
    }
}
