package com.example.portal.console.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.portal.common.cache.PermissionCacheManager;
import com.example.portal.common.model.common.PageResult;
import com.example.portal.common.model.enums.CommonConstant;
import com.example.portal.common.model.entity.BizAdmin;
import com.example.portal.console.mapper.BizAdminMapper;
import com.example.portal.console.service.BizAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 业务管理员服务实现，管理增/删/改/查和缓存清理。
 */
@Service
@RequiredArgsConstructor
public class BizAdminServiceImpl implements BizAdminService {

    private final BizAdminMapper bizAdminMapper;
    private final PermissionCacheManager cacheManager;

    @Override
    public PageResult<BizAdmin> list(String appCode, int pageNum, int pageSize) {
        LambdaQueryWrapper<BizAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizAdmin::getAppCode, appCode);
        wrapper.orderByDesc(BizAdmin::getCreateTime);

        Page<BizAdmin> page = bizAdminMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    @Override
    public void add(String appCode, List<String> userIds) {
        List<BizAdmin> admins = userIds.stream().map(userId -> {
            BizAdmin admin = new BizAdmin();
            admin.setAppCode(appCode);
            admin.setUserId(userId);
            admin.setManageScope(CommonConstant.SCOPE_APP);
            admin.setStatus(CommonConstant.STATUS_ENABLED);
            return admin;
        }).collect(Collectors.toList());
        admins.forEach(bizAdminMapper::insert);
        cacheManager.evictUsers(userIds);
    }

    @Override
    public void disable(String appCode, String userId) {
        LambdaUpdateWrapper<BizAdmin> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BizAdmin::getAppCode, appCode)
                .eq(BizAdmin::getUserId, userId)
                .set(BizAdmin::getStatus, CommonConstant.STATUS_DISABLED)
                .set(BizAdmin::getUpdateTime, LocalDateTime.now());
        bizAdminMapper.update(null, wrapper);
        cacheManager.evictUser(userId);
    }

    @Override
    public void enable(String appCode, String userId) {
        LambdaUpdateWrapper<BizAdmin> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BizAdmin::getAppCode, appCode)
                .eq(BizAdmin::getUserId, userId)
                .set(BizAdmin::getStatus, CommonConstant.STATUS_ENABLED)
                .set(BizAdmin::getUpdateTime, LocalDateTime.now());
        bizAdminMapper.update(null, wrapper);
        cacheManager.evictUser(userId);
    }

    @Override
    public void remove(String appCode, String userId) {
        LambdaQueryWrapper<BizAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizAdmin::getAppCode, appCode)
                .eq(BizAdmin::getUserId, userId);
        bizAdminMapper.delete(wrapper);
        cacheManager.evictUser(userId);
    }

    @Override
    public void removeBatch(String appCode, List<String> userIds) {
        LambdaQueryWrapper<BizAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizAdmin::getAppCode, appCode)
                .in(BizAdmin::getUserId, userIds);
        bizAdminMapper.delete(wrapper);
        cacheManager.evictUsers(userIds);
    }
}
