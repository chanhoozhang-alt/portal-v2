package com.example.portal.portal.service;

import com.example.portal.common.model.dto.portal.PortalInitResponse;

/**
 * 门户首页服务接口：提供用户初始化、应用列表、权限信息和跳转信息。
 */
public interface PortalService {

    PortalInitResponse init();

    PortalInitResponse apps();

    PortalInitResponse adminPermission();

    PortalInitResponse.AppItem jumpInfo(String appCode);
}
