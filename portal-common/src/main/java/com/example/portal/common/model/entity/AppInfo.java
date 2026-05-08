package com.example.portal.common.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/** 应用信息主表，记录应用基本配置、可见性和客户端认证信息。 */
@Data
@TableName("app_info")
public class AppInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String appCode;
    private String appName;
    private String appIcon;
    private String appDesc;
    private String appUrl;
    private String clientId;
    private String clientSecret;
    private String orgId;
    private String orgName;
    /** ALL-全部可见, ROLE-按角色可见 */
    private String visibleType;
    /** 是否在菜单展示(1-是,0-否) */
    private Integer showMenu;
    /** 是否显示头部(1-是,0-否) */
    private Integer showHeader;
    /** 是否启用 watermark(1-是,0-否) */
    private Integer enableWatermark;
    /** 是否启用推广(1-是,0-否) */
    private Integer enablePromotion;
    /** 排序号 */
    private Integer sortNo;
    /** ENABLED-启用, DISABLED-停用 */
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private String createBy;
    @TableField(fill = FieldFill.INSERT)
    private String createName;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateName;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
