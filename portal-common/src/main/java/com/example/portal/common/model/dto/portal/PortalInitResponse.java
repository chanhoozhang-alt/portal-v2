package com.example.portal.common.model.dto.portal;

import com.example.portal.common.model.common.AppBrief;
import lombok.Data;

import java.util.List;

/** 门户首页初始化响应，包含用户信息、管理员标识和分组后的可见应用。 */
@Data
public class PortalInitResponse {

    private UserInfo user;
    private boolean isAdmin;
    private boolean isSystemAdmin;
    private List<AppBrief> appAdminApps;
    private List<AppBrief> bizAdminApps;
    private List<AppGroup> visibleApps;

    @Data
    public static class UserInfo {
        private String userId;
        private String userName;
        private String orgName;
        private String deptName;
    }

    @Data
    public static class AppGroup {
        private String groupCode;
        private String groupName;
        private Integer groupSortNo;
        private List<AppItem> apps;
    }

        @Data
    public static class AppItem {
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
        /** 应用排序号 */
        private Integer appSortNo;
    }
}
