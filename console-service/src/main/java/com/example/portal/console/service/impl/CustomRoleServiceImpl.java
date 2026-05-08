package com.example.portal.console.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.portal.common.cache.PermissionCacheManager;
import com.example.portal.common.model.entity.AppCustomRole;
import com.example.portal.common.model.enums.CommonConstant;
import com.example.portal.console.mapper.AppCustomRoleMapper;
import com.example.portal.console.service.CustomRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自定义角色服务实现，管理角色的增删改查和启用/停用。
 */
@Service
@RequiredArgsConstructor
public class CustomRoleServiceImpl implements CustomRoleService {

    private final AppCustomRoleMapper customRoleMapper;
    private final PermissionCacheManager cacheManager;

    @Override
    public List<AppCustomRole> list(String appCode, String roleCode, String roleName, String status) {
        LambdaQueryWrapper<AppCustomRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppCustomRole::getAppCode, appCode);
        if (StringUtils.hasText(roleCode)) {
            wrapper.like(AppCustomRole::getRoleCode, roleCode);
        }
        if (StringUtils.hasText(roleName)) {
            wrapper.like(AppCustomRole::getRoleName, roleName);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(AppCustomRole::getStatus, status);
        }
        wrapper.orderByDesc(AppCustomRole::getCreateTime);
        return customRoleMapper.selectList(wrapper);
    }

    @Override
    public void add(String appCode, String roleCode, String roleName, String roleDesc) {
        AppCustomRole role = new AppCustomRole();
        role.setAppCode(appCode);
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setRoleDesc(roleDesc);
        role.setStatus(CommonConstant.STATUS_ENABLED);
        customRoleMapper.insert(role);
        cacheManager.evictAllVisibleApps();
    }

    @Override
    public void update(String appCode, String roleCode, String roleName, String roleDesc) {
        LambdaUpdateWrapper<AppCustomRole> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AppCustomRole::getAppCode, appCode)
                .eq(AppCustomRole::getRoleCode, roleCode)
                .set(AppCustomRole::getRoleName, roleName)
                .set(AppCustomRole::getRoleDesc, roleDesc);
        customRoleMapper.update(null, wrapper);
        cacheManager.evictAllVisibleApps();
    }

    @Override
    public void disable(String appCode, String roleCode) {
        LambdaUpdateWrapper<AppCustomRole> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AppCustomRole::getAppCode, appCode)
                .eq(AppCustomRole::getRoleCode, roleCode)
                .set(AppCustomRole::getStatus, CommonConstant.STATUS_DISABLED)
                .set(AppCustomRole::getUpdateTime, LocalDateTime.now());
        customRoleMapper.update(null, wrapper);
        cacheManager.evictAllVisibleApps();
    }

    @Override
    public void enable(String appCode, String roleCode) {
        LambdaUpdateWrapper<AppCustomRole> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AppCustomRole::getAppCode, appCode)
                .eq(AppCustomRole::getRoleCode, roleCode)
                .set(AppCustomRole::getStatus, CommonConstant.STATUS_ENABLED)
                .set(AppCustomRole::getUpdateTime, LocalDateTime.now());
        customRoleMapper.update(null, wrapper);
        cacheManager.evictAllVisibleApps();
    }
}
