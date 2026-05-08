package com.example.portal.console.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.portal.common.cache.PermissionCacheManager;
import com.example.portal.common.context.UserContext;
import com.example.portal.common.model.enums.CommonConstant;
import com.example.portal.common.exception.BusinessException;
import com.example.portal.common.model.common.AppBrief;
import com.example.portal.common.model.common.PageResult;
import com.example.portal.common.model.entity.AppCustomRole;
import com.example.portal.common.model.entity.AppInfo;
import com.example.portal.common.model.entity.UserRole;
import com.example.portal.console.mapper.AppCustomRoleMapper;
import com.example.portal.console.mapper.AppInfoMapper;
import com.example.portal.console.mapper.UserRoleMapper;
import com.example.portal.console.service.RoleUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户角色分配服务实现，含批量添加去重和业务管理员管辖角色查询。
 */
@Service
@RequiredArgsConstructor
public class RoleUserServiceImpl implements RoleUserService {

    private final UserRoleMapper userRoleMapper;
    private final AppCustomRoleMapper appCustomRoleMapper;
    private final AppInfoMapper appInfoMapper;
    private final PermissionCacheManager cacheManager;

    @Override
    public PageResult<UserRole> listUsers(String appCode, String roleCode, String userName, String userId, String status, int pageNum, int pageSize) {
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getAppCode, appCode);
        wrapper.eq(UserRole::getRoleCode, roleCode);
        if (StringUtils.hasText(userName)) {
            wrapper.like(UserRole::getUserName, userName);
        }
        if (StringUtils.hasText(userId)) {
            wrapper.eq(UserRole::getUserId, userId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(UserRole::getStatus, status);
        }
        wrapper.orderByDesc(UserRole::getGrantTime);

        Page<UserRole> page = userRoleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    @Override
    public void addUsers(String appCode, String roleCode, List<String> userIds) {
        UserContext ctx = UserContext.get();

        // 预查已有 userId，跳过已存在的记录实现幂等
        List<UserRole> existing = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getAppCode, appCode)
                        .eq(UserRole::getRoleCode, roleCode)
                        .in(UserRole::getUserId, userIds));
        Set<String> existingUserIds = existing.stream()
                .map(UserRole::getUserId)
                .collect(Collectors.toSet());

        Date now = new Date();
        List<String> newUserIds = new ArrayList<>();
        for (String uid : userIds) {
            if (existingUserIds.contains(uid)) {
                continue;
            }
            UserRole userRole = new UserRole();
            userRole.setAppCode(appCode);
            userRole.setRoleCode(roleCode);
            userRole.setUserId(uid);
            userRole.setStatus(CommonConstant.STATUS_ENABLED);
            userRole.setGrantBy(ctx.getUserId());
            userRole.setGrantName(ctx.getUserName());
            userRole.setGrantTime(now);
            userRoleMapper.insert(userRole);
            newUserIds.add(uid);
        }
        if (!newUserIds.isEmpty()) {
            cacheManager.evictUsers(newUserIds);
        }
    }

    @Override
    public void disableUser(String appCode, String roleCode, String userId) {
        UserRole userRole = getOne(appCode, roleCode, userId);
        if (userRole == null) {
            throw new BusinessException("用户角色记录不存在");
        }
        UserContext ctx = UserContext.get();
        userRole.setStatus(CommonConstant.STATUS_DISABLED);
        userRole.setRevokeBy(ctx.getUserId());
        userRole.setRevokeName(ctx.getUserName());
        userRole.setRevokeTime(new Date());
        userRoleMapper.updateById(userRole);
        cacheManager.evictUser(userId);
    }

    @Override
    public void enableUser(String appCode, String roleCode, String userId) {
        UserRole userRole = getOne(appCode, roleCode, userId);
        if (userRole == null) {
            throw new BusinessException("用户角色记录不存在");
        }
        userRole.setStatus(CommonConstant.STATUS_ENABLED);
        userRoleMapper.updateById(userRole);
        cacheManager.evictUser(userId);
    }

    @Override
    public void batchRemoveUsers(String appCode, String roleCode, List<String> userIds) {
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getAppCode, appCode)
                .eq(UserRole::getRoleCode, roleCode)
                .in(UserRole::getUserId, userIds);
        userRoleMapper.delete(wrapper);
        cacheManager.evictUsers(userIds);
    }

    /**
     * 查询当前业务管理员所管辖应用的自定义角色列表。
     * 返回结构：应用（含应用信息）→ 该应用下启用的角色列表，用于业务管理员在管理台分配角色。
     */
    @Override
    public List<Map<String, Object>> listBizManageRoles() {
        UserContext ctx = UserContext.get();
        List<AppBrief> bizAdminApps = ctx.getBizAdminApps();
        if (bizAdminApps == null || bizAdminApps.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (AppBrief appBrief : bizAdminApps) {
            String appCode = appBrief.getAppCode();

            // 查询应用信息
            AppInfo appInfo = appInfoMapper.selectOne(
                    new LambdaQueryWrapper<AppInfo>().eq(AppInfo::getAppCode, appCode));
            if (appInfo == null) {
                continue;
            }

            // 查询该应用下启用的自定义角色
            List<AppCustomRole> roles = appCustomRoleMapper.selectList(
                    new LambdaQueryWrapper<AppCustomRole>()
                            .eq(AppCustomRole::getAppCode, appCode)
                            .eq(AppCustomRole::getStatus, CommonConstant.STATUS_ENABLED));

            List<Map<String, Object>> roleList = roles.stream().map(role -> {
                Map<String, Object> roleMap = new LinkedHashMap<>();
                roleMap.put("roleCode", role.getRoleCode());
                roleMap.put("roleName", role.getRoleName());
                roleMap.put("status", role.getStatus());
                return roleMap;
            }).collect(Collectors.toList());

            Map<String, Object> appMap = new LinkedHashMap<>();
            appMap.put("appCode", appCode);
            appMap.put("appName", appInfo.getAppName());
            appMap.put("roles", roleList);
            result.add(appMap);
        }
        return result;
    }

    private UserRole getOne(String appCode, String roleCode, String userId) {
        return userRoleMapper.selectOne(
                new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getAppCode, appCode)
                        .eq(UserRole::getRoleCode, roleCode)
                        .eq(UserRole::getUserId, userId));
    }
}
