package com.example.portal.console.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.portal.common.cache.PermissionCacheManager;
import com.example.portal.common.model.common.PageResult;
import com.example.portal.common.model.enums.CommonConstant;
import com.example.portal.common.model.entity.AppAdmin;
import com.example.portal.console.mapper.AppAdminMapper;
import com.example.portal.console.service.AppAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import java.util.List;

/**
 * 应用管理员服务实现，管理应用的增/删/改/查和缓存清理。
 */
@Service
@RequiredArgsConstructor
public class AppAdminServiceImpl implements AppAdminService {

    private final AppAdminMapper appAdminMapper;
    private final PermissionCacheManager cacheManager;

    @Override
    public PageResult<AppAdmin> list(String appCode, int pageNum, int pageSize) {
        LambdaQueryWrapper<AppAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppAdmin::getAppCode, appCode);
        wrapper.orderByDesc(AppAdmin::getCreateTime);

        Page<AppAdmin> page = appAdminMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    @Override
    public void add(String appCode, List<String> userIds) {
        List<AppAdmin> admins = userIds.stream().map(userId -> {
            AppAdmin admin = new AppAdmin();
            admin.setAppCode(appCode);
            admin.setUserId(userId);
            admin.setStatus(CommonConstant.STATUS_ENABLED);
            return admin;
        }).collect(java.util.stream.Collectors.toList());
        admins.forEach(appAdminMapper::insert);
        cacheManager.evictUsers(userIds);
    }

    @Override
    public void disable(String appCode, String userId) {
        LambdaUpdateWrapper<AppAdmin> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AppAdmin::getAppCode, appCode)
                .eq(AppAdmin::getUserId, userId)
                .set(AppAdmin::getStatus, CommonConstant.STATUS_DISABLED)
                .set(AppAdmin::getUpdateTime, LocalDateTime.now());
        appAdminMapper.update(null, wrapper);
        cacheManager.evictUser(userId);
    }

    @Override
    public void enable(String appCode, String userId) {
        LambdaUpdateWrapper<AppAdmin> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AppAdmin::getAppCode, appCode)
                .eq(AppAdmin::getUserId, userId)
                .set(AppAdmin::getStatus, CommonConstant.STATUS_ENABLED)
                .set(AppAdmin::getUpdateTime, LocalDateTime.now());
        appAdminMapper.update(null, wrapper);
        cacheManager.evictUser(userId);
    }

    @Override
    public void remove(String appCode, String userId) {
        LambdaQueryWrapper<AppAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppAdmin::getAppCode, appCode)
                .eq(AppAdmin::getUserId, userId);
        appAdminMapper.delete(wrapper);
        cacheManager.evictUser(userId);
    }

    @Override
    public void removeBatch(String appCode, List<String> userIds) {
        LambdaQueryWrapper<AppAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppAdmin::getAppCode, appCode)
                .in(AppAdmin::getUserId, userIds);
        appAdminMapper.delete(wrapper);
        cacheManager.evictUsers(userIds);
    }
}
