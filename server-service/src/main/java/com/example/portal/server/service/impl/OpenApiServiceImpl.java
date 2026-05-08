package com.example.portal.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.portal.common.exception.ForbiddenException;
import com.example.portal.common.model.dto.server.OpenAccessResponse;
import com.example.portal.common.model.enums.CommonConstant;
import com.example.portal.common.model.dto.server.OpenRoleResponse;
import com.example.portal.common.model.entity.AppInfo;
import com.example.portal.common.model.entity.UserRole;
import com.example.portal.server.mapper.AppInfoMapper;
import com.example.portal.server.mapper.UserRoleMapper;
import com.example.portal.server.service.OpenApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 三方开放 API 服务实现，提供用户角色查询和访问权限校验。
 * 通过 ClientId/ClientSecret/appCode 三重认证。
 */
@Service
@RequiredArgsConstructor
public class OpenApiServiceImpl implements OpenApiService {

    private final AppInfoMapper appInfoMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    public OpenRoleResponse queryUserRoles(String clientId, String clientSecret, String appCode, String userId) {
        AppInfo app = verifyClient(clientId, clientSecret, appCode);

        List<UserRole> roles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getAppCode, appCode)
                        .eq(UserRole::getUserId, userId)
                        .eq(UserRole::getStatus, CommonConstant.STATUS_ENABLED));

        OpenRoleResponse resp = new OpenRoleResponse();
        resp.setAppCode(appCode);
        resp.setUserId(userId);
        resp.setRoles(roles.stream().map(r -> {
            OpenRoleResponse.RoleItem item = new OpenRoleResponse.RoleItem();
            item.setRoleCode(r.getRoleCode());
            item.setRoleName(null);
            return item;
        }).collect(Collectors.toList()));
        return resp;
    }

    @Override
    public OpenAccessResponse checkUserAccess(String clientId, String clientSecret, String appCode, String userId) {
        verifyClient(clientId, clientSecret, appCode);

        int count = userRoleMapper.countEnabledByAppAndUser(appCode, userId);

        OpenAccessResponse resp = new OpenAccessResponse();
        resp.setAppCode(appCode);
        resp.setUserId(userId);
        resp.setHasAccess(count > 0);
        return resp;
    }

    /**
     * 三方客户端认证：按 clientId 查出应用，校验 clientSecret 和 appCode 是否匹配。
     */
    private AppInfo verifyClient(String clientId, String clientSecret, String appCode) {
        AppInfo app = appInfoMapper.selectOne(
                new LambdaQueryWrapper<AppInfo>()
                        .eq(AppInfo::getClientId, clientId)
                        .last("LIMIT 1"));
        if (app == null) {
            throw new ForbiddenException("无效的ClientId");
        }
        if (!app.getClientSecret().equals(clientSecret)) {
            throw new ForbiddenException("ClientSecret不匹配");
        }
        if (!app.getAppCode().equals(appCode)) {
            throw new ForbiddenException("appCode与ClientId不匹配");
        }
        return app;
    }
}
