package com.example.portal.common.model.common;

import lombok.Data;

/**
 * 应用简要信息，仅含 appCode 和 appName，用于管理员列表展示。
 */
@Data
public class AppBrief {

    private String appCode;
    private String appName;

    public AppBrief() {}

    public AppBrief(String appCode, String appName) {
        this.appCode = appCode;
        this.appName = appName;
    }
}
