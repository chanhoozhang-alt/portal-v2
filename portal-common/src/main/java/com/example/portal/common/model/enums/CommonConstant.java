package com.example.portal.common.model.enums;

/**
 * 通用状态常量，统一管理项目中硬编码的状态字符串。
 */
public class CommonConstant {

    private CommonConstant() {}

    /** 启用 */
    public static final String STATUS_ENABLED = "ENABLED";
    /** 停用 */
    public static final String STATUS_DISABLED = "DISABLED";

    /** 应用级管理 */
    public static final String SCOPE_APP = "APP";
    /** 角色级管理 */
    public static final String SCOPE_ROLE = "ROLE";

    /** 全部可见 */
    public static final String VISIBLE_ALL = "ALL";
    /** 按角色可见 */
    public static final String VISIBLE_ROLE = "ROLE";

    /** 操作成功 */
    public static final String RESULT_SUCCESS = "SUCCESS";
    /** 操作失败 */
    public static final String RESULT_FAIL = "FAIL";
}
