package com.example.portal.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.portal.common.cache.PermissionCacheManager;
import com.example.portal.common.exception.UnauthorizedException;
import com.example.portal.common.model.enums.CommonConstant;
import com.example.portal.common.model.common.AppBrief;
import com.example.portal.common.model.dto.server.AuthInitResponse;
import com.example.portal.common.model.entity.AppAdmin;
import com.example.portal.common.model.entity.AppInfo;
import com.example.portal.common.model.entity.BizAdmin;
import com.example.portal.common.model.entity.SystemAdmin;
import com.example.portal.server.client.IdentityPlatformClient;
import com.example.portal.server.mapper.AppAdminMapper;
import com.example.portal.server.mapper.AppInfoMapper;
import com.example.portal.server.mapper.BizAdminMapper;
import com.example.portal.server.mapper.SystemAdminMapper;
import com.example.portal.server.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 认证服务实现，处理用户 Token 验证、权限查询和缓存写入。
 * 作为整个系统的认证数据源，Console 和 Portal 模块依赖本类写入 Redis 的数据。
 *
 * <p>核心流程：
 * <ol>
 *   <li>调用身份平台验证 Token，获取用户基本信息</li>
 *   <li>查询三张管理员表（系统/应用/业务），确定用户角色</li>
 *   <li>批量补全应用名称</li>
 *   <li>查询可见应用列表</li>
 *   <li>构建认证响应并写入 Redis 缓存（Token 映射、身份 JSON、可见应用）</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final IdentityPlatformClient identityPlatformClient;
    private final SystemAdminMapper systemAdminMapper;
    private final AppAdminMapper appAdminMapper;
    private final BizAdminMapper bizAdminMapper;
    private final AppInfoMapper appInfoMapper;
    private final PermissionCacheManager cacheManager;

    @Override
    public AuthInitResponse init(String token, String idToken) {
        // 第 1 步：外部身份平台校验 Token，获取用户基础信息
        IdentityPlatformClient.IdentityInfo identity = identityPlatformClient.verifyToken(token, idToken);
        if (identity == null) {
            throw new UnauthorizedException("Token验证失败");
        }

        String userId = identity.getUserId();

        // 第 2 步：查系统管理员
        boolean isSystemAdmin = systemAdminMapper.selectCount(
                new LambdaQueryWrapper<SystemAdmin>()
                        .eq(SystemAdmin::getUserId, userId)
                        .eq(SystemAdmin::getStatus, CommonConstant.STATUS_ENABLED)) > 0;

        // 2. 查应用管理员
        List<AppAdmin> appAdmins = appAdminMapper.selectList(
                new LambdaQueryWrapper<AppAdmin>()
                        .eq(AppAdmin::getUserId, userId)
                        .eq(AppAdmin::getStatus, CommonConstant.STATUS_ENABLED));
        List<AppBrief> appAdminApps = appAdmins.stream()
                .map(a -> new AppBrief(a.getAppCode(), null))
                .collect(Collectors.toList());

        // 3. 查业务管理员
        List<BizAdmin> bizAdmins = bizAdminMapper.selectList(
                new LambdaQueryWrapper<BizAdmin>()
                        .eq(BizAdmin::getUserId, userId)
                        .eq(BizAdmin::getStatus, CommonConstant.STATUS_ENABLED));
        List<AppBrief> bizAdminApps = bizAdmins.stream()
                .map(b -> new AppBrief(b.getAppCode(), null))
                .collect(Collectors.toList());

        // 第 4 步：合并两个管理员列表的 appCode，一次批量查完名称（避免 N+1 查询）
        fillAppNamesBatch(appAdminApps, bizAdminApps);

        // 第 5 步：查询用户可见的完整应用列表
        List<AppInfo> visibleAppInfos = appInfoMapper.selectVisibleApps(userId);
        List<AuthInitResponse.VisibleApp> visibleApps = visibleAppInfos.stream()
                .map(this::toVisibleApp)
                .collect(Collectors.toList());

        // 第 6 步：系统管理员、应用管理员或业务管理员任一成立即为 admin
        boolean isAdmin = isSystemAdmin || !appAdminApps.isEmpty() || !bizAdminApps.isEmpty();

        // 第 7 步：组装响应
        AuthInitResponse resp = new AuthInitResponse();
        resp.setUserId(userId);
        resp.setUserName(identity.getUserName());
        resp.setOrgId(identity.getOrgId());
        resp.setOrgName(identity.getOrgName());
        resp.setDeptId(identity.getDeptId());
        resp.setDeptName(identity.getDeptName());
        resp.setAdmin(isAdmin);
        resp.setSystemAdmin(isSystemAdmin);
        resp.setAppAdminApps(appAdminApps);
        resp.setBizAdminApps(bizAdminApps);
        resp.setVisibleApps(visibleApps);

        // 第 8 步：写入 Redis — Token 映射、身份信息、可见应用，三者 TTL 一致

        // 构建 identity JSON（不含 visibleApps）
        IdentityCache identityCache = new IdentityCache();
        identityCache.setUserId(userId);
        identityCache.setUserName(identity.getUserName());
        identityCache.setOrgId(identity.getOrgId());
        identityCache.setOrgName(identity.getOrgName());
        identityCache.setDeptId(identity.getDeptId());
        identityCache.setDeptName(identity.getDeptName());
        identityCache.setAdmin(isAdmin);
        identityCache.setSystemAdmin(isSystemAdmin);
        identityCache.setAppAdminApps(appAdminApps);
        identityCache.setBizAdminApps(bizAdminApps);
        cacheManager.setIdentity(userId, identityCache);
        cacheManager.setVisibleApps(userId, visibleApps);
        cacheManager.setToken(token, userId);

        return resp;
    }

    /**
     * 批量补全应用名称。合并应用管理员和业务管理员的 appCode 集合，
     * 一次 SQL 查询取回所有名称，避免对每个 appCode 单独查询。
     */
    private void fillAppNamesBatch(List<AppBrief> appAdminApps, List<AppBrief> bizAdminApps) {
        // 收集所有不重复的 appCode
        Set<String> allCodes = new HashSet<>();
        if (appAdminApps != null) appAdminApps.forEach(a -> allCodes.add(a.getAppCode()));
        if (bizAdminApps != null) bizAdminApps.forEach(a -> allCodes.add(a.getAppCode()));
        if (allCodes.isEmpty()) return;

        // 一次批量查询取回所有名称
        List<AppInfo> appInfos = appInfoMapper.selectList(
                new LambdaQueryWrapper<AppInfo>()
                        .select(AppInfo::getAppCode, AppInfo::getAppName)
                        .in(AppInfo::getAppCode, allCodes));
        Map<String, String> nameMap = appInfos.stream()
                .collect(Collectors.toMap(AppInfo::getAppCode, AppInfo::getAppName, (a, b) -> a));

        // 回填名称
        if (appAdminApps != null) appAdminApps.forEach(a -> a.setAppName(nameMap.get(a.getAppCode())));
        if (bizAdminApps != null) bizAdminApps.forEach(a -> a.setAppName(nameMap.get(a.getAppCode())));
    }

    /**
     * 将 DB 实体 AppInfo 转换为可见应用响应对象，
     * 将 Integer 的 0/1 标记转换为 boolean 字段。
     */
    private AuthInitResponse.VisibleApp toVisibleApp(AppInfo a) {
        AuthInitResponse.VisibleApp v = new AuthInitResponse.VisibleApp();
        v.setAppCode(a.getAppCode());
        v.setAppName(a.getAppName());
        v.setAppIcon(a.getAppIcon());
        v.setAppDesc(a.getAppDesc());
        v.setJumpUrl(a.getAppUrl());
        v.setVisibleType(a.getVisibleType());
        v.setShowMenu(a.getShowMenu() != null && a.getShowMenu() == 1);
        v.setShowHeader(a.getShowHeader() != null && a.getShowHeader() == 1);
        v.setEnableWatermark(a.getEnableWatermark() != null && a.getEnableWatermark() == 1);
        return v;
    }

    @lombok.Data
    private static class IdentityCache {
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
    }
}
