package com.example.portal.console.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.portal.common.cache.PermissionCacheManager;
import com.example.portal.common.exception.BusinessException;
import com.example.portal.common.model.enums.CommonConstant;
import com.example.portal.common.model.common.PageResult;
import com.example.portal.common.model.entity.SystemAdmin;
import com.example.portal.console.mapper.SystemAdminMapper;
import com.example.portal.console.service.SystemAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统管理员服务实现，管理增/删/改/查和缓存清理。
 */
@Service
@RequiredArgsConstructor
public class SystemAdminServiceImpl implements SystemAdminService {

    private final SystemAdminMapper systemAdminMapper;
    private final PermissionCacheManager cacheManager;

    @Override
    public PageResult<SystemAdmin> list(String userName, String userId, int pageNum, int pageSize) {
        LambdaQueryWrapper<SystemAdmin> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(userName)) {
            wrapper.like(SystemAdmin::getUserName, userName);
        }
        if (StringUtils.hasText(userId)) {
            wrapper.eq(SystemAdmin::getUserId, userId);
        }
        wrapper.orderByDesc(SystemAdmin::getCreateTime);

        Page<SystemAdmin> page = systemAdminMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    @Override
    public void add(List<String> userIds) {
        // 预查已有 userId，跳过已存在的记录
        List<SystemAdmin> existing = systemAdminMapper.selectList(
                new LambdaQueryWrapper<SystemAdmin>().in(SystemAdmin::getUserId, userIds));
        List<String> existingIds = existing.stream()
                .map(SystemAdmin::getUserId)
                .collect(Collectors.toList());
        List<String> newUserIds = new ArrayList<>();
        for (String uid : userIds) {
            if (existingIds.contains(uid)) {
                continue;
            }
            SystemAdmin admin = new SystemAdmin();
            admin.setUserId(uid);
            admin.setStatus(CommonConstant.STATUS_ENABLED);
            systemAdminMapper.insert(admin);
            newUserIds.add(uid);
        }
        if (!newUserIds.isEmpty()) {
            cacheManager.evictUsers(newUserIds);
        }
    }

    @Override
    public void disable(String userId) {
        SystemAdmin admin = getByUserId(userId);
        if (admin == null) {
            throw new BusinessException("系统管理员不存在");
        }
        admin.setStatus(CommonConstant.STATUS_DISABLED);
        systemAdminMapper.updateById(admin);
        cacheManager.evictUser(userId);
    }

    @Override
    public void enable(String userId) {
        SystemAdmin admin = getByUserId(userId);
        if (admin == null) {
            throw new BusinessException("系统管理员不存在");
        }
        admin.setStatus(CommonConstant.STATUS_ENABLED);
        systemAdminMapper.updateById(admin);
        cacheManager.evictUser(userId);
    }

    @Override
    public void remove(String userId) {
        systemAdminMapper.delete(
                new LambdaQueryWrapper<SystemAdmin>().eq(SystemAdmin::getUserId, userId));
        cacheManager.evictUser(userId);
    }

    private SystemAdmin getByUserId(String userId) {
        return systemAdminMapper.selectOne(
                new LambdaQueryWrapper<SystemAdmin>().eq(SystemAdmin::getUserId, userId));
    }
}
