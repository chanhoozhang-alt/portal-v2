package com.example.portal.server.service;

import com.example.portal.common.model.dto.server.OpenAccessResponse;
import com.example.portal.common.model.dto.server.OpenRoleResponse;

/**
 * 三方开放 API 服务接口：供外部应用查询用户角色和访问权限。
 */
public interface OpenApiService {

    OpenRoleResponse queryUserRoles(String clientId, String clientSecret, String appCode, String userId);

    OpenAccessResponse checkUserAccess(String clientId, String clientSecret, String appCode, String userId);
}
