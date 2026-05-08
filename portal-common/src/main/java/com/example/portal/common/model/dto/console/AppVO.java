package com.example.portal.common.model.dto.console;

import com.example.portal.common.model.entity.AppInfo;
import lombok.Data;

import java.util.Date;

/**
 * 应用信息视图对象，用于 Controller 层返回给前端。
 * 隐藏 id、clientSecret 等内部字段，将 appUrl 转为 jumpUrl，Integer 标志位转为 Boolean。
 */
@Data
public class AppVO {

    private String appCode;
    private String appName;
    private String appIcon;
    private String appDesc;
    /** 跳转地址（对应数据库 app_url） */
    private String jumpUrl;
    private String clientId;
    private String orgId;
    private String orgName;
    private String visibleType;
    private Boolean showMenu;
    private Boolean showHeader;
    private Boolean enableWatermark;
    private Boolean enablePromotion;
    private Integer sortNo;
    private String status;
    private String createBy;
    private String createName;
    private Date createTime;
    private String updateBy;
    private String updateName;
    private Date updateTime;

    public static AppVO from(AppInfo entity) {
        if (entity == null) {
            return null;
        }
        AppVO vo = new AppVO();
        vo.setAppCode(entity.getAppCode());
        vo.setAppName(entity.getAppName());
        vo.setAppIcon(entity.getAppIcon());
        vo.setAppDesc(entity.getAppDesc());
        vo.setJumpUrl(entity.getAppUrl());
        vo.setClientId(entity.getClientId());
        vo.setOrgId(entity.getOrgId());
        vo.setOrgName(entity.getOrgName());
        vo.setVisibleType(entity.getVisibleType());
        vo.setShowMenu(entity.getShowMenu() != null && entity.getShowMenu() == 1);
        vo.setShowHeader(entity.getShowHeader() != null && entity.getShowHeader() == 1);
        vo.setEnableWatermark(entity.getEnableWatermark() != null && entity.getEnableWatermark() == 1);
        vo.setEnablePromotion(entity.getEnablePromotion() != null && entity.getEnablePromotion() == 1);
        vo.setSortNo(entity.getSortNo());
        vo.setStatus(entity.getStatus());
        vo.setCreateBy(entity.getCreateBy());
        vo.setCreateName(entity.getCreateName());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateBy(entity.getUpdateBy());
        vo.setUpdateName(entity.getUpdateName());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
