package com.example.portal.common.model.dto.console;

import lombok.Data;

/** 应用新增/修改请求参数。 */
@Data
public class AppSaveRequest {

    private String appCode;
    private String appName;
    private String appIcon;
    private String appDesc;
    private String jumpUrl;
    private String orgId;
    private String orgName;
    /** ALL-全部可见, ROLE-按角色可见 */
    private String visibleType;
    /** 是否在菜单展示 */
    private Boolean showMenu;
    /** 是否显示头部 */
    private Boolean showHeader;
    /** 是否启用 watermark */
    private Boolean enableWatermark;
    /** 是否启用推广 */
    private Boolean enablePromotion;
    /** 排序号(越小越靠前) */
    private Integer sortNo;
}
