package com.example.portal.common.model.dto.server;

import com.example.portal.common.model.common.AppBrief;
import lombok.Data;

import java.util.List;

/** 认证初始化响应，包含用户身份、角色和可见应用列表。 */
@Data
public class AuthInitResponse {

    private String userId;
    private String userName;
    private String orgId;
    private String orgName;
    private String deptId;
    private String deptName;
    private boolean isAdmin;
    private boolean isSystemAdmin;
    private List<AppBrief> appAdminApps;
    private List<AppBrief> bizAdminApps;
    private List<VisibleApp> visibleApps;

        @Data
    public static class VisibleApp {
        private String appCode;
        private String appName;
        private String appIcon;
        private String appDesc;
        private String jumpUrl;
        /** ALL-全部可见, ROLE-按角色可见 */
        private String visibleType;
        /** 是否在菜单展示 */
        private Boolean showMenu;
        /** 是否显示头部 */
        private Boolean showHeader;
        /** 是否启用 watermark */
        private Boolean enableWatermark;
        private String groupCode;
        private String groupName;
        /** 分组排序号 */
        private Integer groupSortNo;
        /** 应用排序号 */
        private Integer appSortNo;
    }
}
