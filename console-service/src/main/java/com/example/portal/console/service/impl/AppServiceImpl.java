package com.example.portal.console.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.portal.common.cache.PermissionCacheManager;
import com.example.portal.common.context.UserContext;
import com.example.portal.common.model.enums.CommonConstant;
import com.example.portal.common.exception.BusinessException;
import com.example.portal.common.model.common.PageResult;
import com.example.portal.common.model.dto.console.AppSaveRequest;
import com.example.portal.common.model.entity.AppInfo;
import com.example.portal.console.mapper.AppInfoMapper;
import com.example.portal.console.service.AppService;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 应用管理服务实现，含数据范围过滤、新增/修改分支、启用/停用和密钥重置。
 */
@Service
@RequiredArgsConstructor
public class AppServiceImpl implements AppService {

    private final AppInfoMapper appInfoMapper;
    private final PermissionCacheManager cacheManager;

    @Override
    public PageResult<AppInfo> list(String appCode, String appName, String status, int pageNum, int pageSize) {
        UserContext ctx = UserContext.get();
        LambdaQueryWrapper<AppInfo> wrapper = new LambdaQueryWrapper<>();

        // 非系统管理员只能看到自己有权限的应用（按数据范围过滤），分页在 DB 层生效
        if (ctx != null && !ctx.isSystemAdmin()) {
            Set<String> userAppCodes = new HashSet<>();
            if (ctx.getAppAdminApps() != null) {
                ctx.getAppAdminApps().forEach(a -> userAppCodes.add(a.getAppCode()));
            }
            if (ctx.getBizAdminApps() != null) {
                ctx.getBizAdminApps().forEach(a -> userAppCodes.add(a.getAppCode()));
            }
            if (userAppCodes.isEmpty()) {
                return new PageResult<>(0L, Collections.emptyList());
            }
            wrapper.in(AppInfo::getAppCode, userAppCodes);
        }

        if (StringUtils.hasText(appCode)) {
            wrapper.like(AppInfo::getAppCode, appCode);
        }
        if (StringUtils.hasText(appName)) {
            wrapper.like(AppInfo::getAppName, appName);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(AppInfo::getStatus, status);
        }
        wrapper.orderByDesc(AppInfo::getUpdateTime);

        Page<AppInfo> page = appInfoMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    @Override
    public AppInfo getByAppCode(String appCode) {
        return appInfoMapper.selectOne(
                new LambdaQueryWrapper<AppInfo>().eq(AppInfo::getAppCode, appCode));
    }

    @Override
    public void save(AppSaveRequest request) {
        // 根据是否有 appCode 区分新增/修改：有 appCode 则为更新，无则为新增
        if (StringUtils.hasText(request.getAppCode())) {
            AppInfo existing = getByAppCode(request.getAppCode());
            if (existing == null) {
                throw new BusinessException("应用不存在");
            }
            updateApp(existing, request);
            appInfoMapper.updateById(existing);
            cacheManager.evictAllVisibleApps();
        } else {
            // 新增
            AppInfo app = new AppInfo();
            app.setAppCode(generateAppCode());
            fillApp(app, request);
            app.setStatus(CommonConstant.STATUS_ENABLED);
            appInfoMapper.insert(app);
            cacheManager.evictAllVisibleApps();
        }
    }

    @Override
    public void disable(String appCode) {
        updateStatus(appCode, CommonConstant.STATUS_DISABLED);
        cacheManager.evictAllVisibleApps();
    }

    @Override
    public void enable(String appCode) {
        updateStatus(appCode, CommonConstant.STATUS_ENABLED);
        cacheManager.evictAllVisibleApps();
    }

    @Override
    public String resetClientSecret(String appCode) {
        AppInfo app = getByAppCode(appCode);
        if (app == null) {
            throw new BusinessException("应用不存在");
        }
        String newSecret = IdUtil.fastSimpleUUID();
        app.setClientSecret(newSecret);
        appInfoMapper.updateById(app);
        return newSecret;
    }

    private void updateStatus(String appCode, String status) {
        AppInfo app = getByAppCode(appCode);
        if (app == null) {
            throw new BusinessException("应用不存在");
        }
        app.setStatus(status);
        appInfoMapper.updateById(app);
    }

    /**
     * 新增填充：无条件设置所有字段，适用于插入场景。
     */
    private void fillApp(AppInfo app, AppSaveRequest req) {
        app.setAppName(req.getAppName());
        app.setAppIcon(req.getAppIcon());
        app.setAppDesc(req.getAppDesc());
        app.setAppUrl(req.getJumpUrl());
        app.setOrgId(req.getOrgId());
        app.setOrgName(req.getOrgName());
        app.setVisibleType(req.getVisibleType());
        app.setShowMenu(req.getShowMenu() != null && req.getShowMenu() ? 1 : 0);
        app.setShowHeader(req.getShowHeader() != null && req.getShowHeader() ? 1 : 0);
        app.setEnableWatermark(req.getEnableWatermark() != null && req.getEnableWatermark() ? 1 : 0);
        app.setEnablePromotion(req.getEnablePromotion() != null && req.getEnablePromotion() ? 1 : 0);
        app.setSortNo(req.getSortNo() != null ? req.getSortNo() : 0);
    }

    /**
     * 更新填充：仅设置请求中非空的字段，支持部分更新。
     */
    private void updateApp(AppInfo app, AppSaveRequest req) {
        if (req.getAppName() != null) app.setAppName(req.getAppName());
        if (req.getAppIcon() != null) app.setAppIcon(req.getAppIcon());
        if (req.getAppDesc() != null) app.setAppDesc(req.getAppDesc());
        if (req.getJumpUrl() != null) app.setAppUrl(req.getJumpUrl());
        if (req.getOrgId() != null) app.setOrgId(req.getOrgId());
        if (req.getOrgName() != null) app.setOrgName(req.getOrgName());
        if (req.getVisibleType() != null) app.setVisibleType(req.getVisibleType());
        if (req.getShowMenu() != null) app.setShowMenu(req.getShowMenu() ? 1 : 0);
        if (req.getShowHeader() != null) app.setShowHeader(req.getShowHeader() ? 1 : 0);
        if (req.getEnableWatermark() != null) app.setEnableWatermark(req.getEnableWatermark() ? 1 : 0);
        if (req.getEnablePromotion() != null) app.setEnablePromotion(req.getEnablePromotion() ? 1 : 0);
        if (req.getSortNo() != null) app.setSortNo(req.getSortNo());
    }

    private String generateAppCode() {
        return "APP" + System.currentTimeMillis();
    }
}
