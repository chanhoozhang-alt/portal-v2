package com.example.portal.console.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.portal.common.cache.PermissionCacheManager;
import com.example.portal.common.exception.BusinessException;
import com.example.portal.common.model.enums.CommonConstant;
import com.example.portal.common.model.dto.console.AppGroupVO;
import com.example.portal.common.model.dto.console.GroupSaveRequest;
import com.example.portal.common.model.dto.console.GroupSortRequest;
import com.example.portal.common.model.entity.AppGroup;
import com.example.portal.common.model.entity.AppGroupRelation;
import com.example.portal.common.model.entity.AppInfo;
import com.example.portal.console.mapper.AppGroupMapper;
import com.example.portal.console.mapper.AppGroupRelationMapper;
import com.example.portal.console.mapper.AppInfoMapper;
import com.example.portal.console.service.AppGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 应用分组服务实现，管理分组增删改查、应用绑定/解绑/排序及缓存清理。
 */
@Service
@RequiredArgsConstructor
public class AppGroupServiceImpl implements AppGroupService {

    private final AppGroupMapper appGroupMapper;
    private final AppGroupRelationMapper appGroupRelationMapper;
    private final AppInfoMapper appInfoMapper;
    private final PermissionCacheManager cacheManager;

    @Override
    public List<AppGroupVO> list() {
        List<AppGroup> groups = appGroupMapper.selectList(
                new LambdaQueryWrapper<AppGroup>()
                        .eq(AppGroup::getStatus, CommonConstant.STATUS_ENABLED)
                        .orderByAsc(AppGroup::getSortNo));

        // 批量查询所有启用分组的应用关联关系
        List<String> groupCodes = groups.stream()
                .map(AppGroup::getGroupCode).collect(Collectors.toList());
        Map<String, List<AppGroupRelation>> relationMap = Map.of();
        if (!groupCodes.isEmpty()) {
            List<AppGroupRelation> allRelations = appGroupRelationMapper.selectList(
                    new LambdaQueryWrapper<AppGroupRelation>()
                            .in(AppGroupRelation::getGroupCode, groupCodes)
                            .orderByAsc(AppGroupRelation::getSortNo));
            relationMap = allRelations.stream()
                    .collect(Collectors.groupingBy(AppGroupRelation::getGroupCode));
        }

        // 批量查询关联应用的信息
        List<String> allAppCodes = relationMap.values().stream()
                .flatMap(List::stream)
                .map(AppGroupRelation::getAppCode)
                .distinct()
                .collect(Collectors.toList());
        Map<String, AppInfo> appInfoMap = Map.of();
        if (!allAppCodes.isEmpty()) {
            List<AppInfo> appInfos = appInfoMapper.selectList(
                    new LambdaQueryWrapper<AppInfo>()
                            .in(AppInfo::getAppCode, allAppCodes));
            appInfoMap = appInfos.stream()
                    .collect(Collectors.toMap(AppInfo::getAppCode, a -> a, (a, b) -> a));
        }

        // 组装 VO
        List<AppGroupVO> result = new ArrayList<>();
        Map<String, List<AppGroupRelation>> finalRelationMap = relationMap;
        Map<String, AppInfo> finalAppInfoMap = appInfoMap;
        for (AppGroup group : groups) {
            AppGroupVO vo = AppGroupVO.from(group);
            List<AppGroupRelation> relations = finalRelationMap.getOrDefault(group.getGroupCode(), new ArrayList<>());
            for (AppGroupRelation rel : relations) {
                AppInfo appInfo = finalAppInfoMap.get(rel.getAppCode());
                AppGroupVO.GroupAppItem item = new AppGroupVO.GroupAppItem();
                item.setAppCode(rel.getAppCode());
                item.setAppName(appInfo != null ? appInfo.getAppName() : null);
                item.setSortNo(rel.getSortNo());
                vo.getApps().add(item);
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public void add(GroupSaveRequest request) {
        AppGroup group = new AppGroup();
        group.setGroupCode(generateGroupCode());
        group.setGroupName(request.getGroupName());
        group.setSortNo(request.getSortNo() != null ? request.getSortNo() : 0);
        group.setOrgId(request.getOrgId());
        group.setOrgName(request.getOrgName());
        group.setStatus(CommonConstant.STATUS_ENABLED);
        appGroupMapper.insert(group);
    }

    @Override
    public void update(String groupCode, GroupSaveRequest request) {
        AppGroup group = getByGroupCode(groupCode);
        if (group == null) {
            throw new BusinessException("分组不存在");
        }
        if (StringUtils.hasText(request.getGroupName())) {
            group.setGroupName(request.getGroupName());
        }
        if (request.getSortNo() != null) {
            group.setSortNo(request.getSortNo());
        }
        if (request.getOrgId() != null) {
            group.setOrgId(request.getOrgId());
        }
        if (request.getOrgName() != null) {
            group.setOrgName(request.getOrgName());
        }
        appGroupMapper.updateById(group);
    }

    @Override
    public void disable(String groupCode) {
        updateStatus(groupCode, CommonConstant.STATUS_DISABLED);
        cacheManager.evictAllVisibleApps();
    }

    @Override
    public void enable(String groupCode) {
        updateStatus(groupCode, CommonConstant.STATUS_ENABLED);
        cacheManager.evictAllVisibleApps();
    }

    @Override
    public void bindApps(String groupCode, List<String> appCodes) {
        for (String appCode : appCodes) {
            // 检查是否已存在
            Long count = appGroupRelationMapper.selectCount(
                    new LambdaQueryWrapper<AppGroupRelation>()
                            .eq(AppGroupRelation::getGroupCode, groupCode)
                            .eq(AppGroupRelation::getAppCode, appCode));
            if (count > 0) {
                continue;
            }
            AppGroupRelation relation = new AppGroupRelation();
            relation.setGroupCode(groupCode);
            relation.setAppCode(appCode);
            relation.setSortNo(0);
            appGroupRelationMapper.insert(relation);
        }
        cacheManager.evictAllVisibleApps();
    }

    @Override
    public void unbindApp(String groupCode, String appCode) {
        appGroupRelationMapper.delete(
                new LambdaQueryWrapper<AppGroupRelation>()
                        .eq(AppGroupRelation::getGroupCode, groupCode)
                        .eq(AppGroupRelation::getAppCode, appCode));
        cacheManager.evictAllVisibleApps();
    }

    @Override
    public void sortApps(String groupCode, GroupSortRequest request) {
        if (request.getSortItems() == null) {
            return;
        }
        for (GroupSortRequest.SortItem item : request.getSortItems()) {
            AppGroupRelation relation = appGroupRelationMapper.selectOne(
                    new LambdaQueryWrapper<AppGroupRelation>()
                            .eq(AppGroupRelation::getGroupCode, groupCode)
                            .eq(AppGroupRelation::getAppCode, item.getAppCode()));
            if (relation != null) {
                relation.setSortNo(item.getSortNo());
                appGroupRelationMapper.updateById(relation);
            }
        }
        cacheManager.evictAllVisibleApps();
    }

    private AppGroup getByGroupCode(String groupCode) {
        return appGroupMapper.selectOne(
                new LambdaQueryWrapper<AppGroup>().eq(AppGroup::getGroupCode, groupCode));
    }

    private void updateStatus(String groupCode, String status) {
        AppGroup group = getByGroupCode(groupCode);
        if (group == null) {
            throw new BusinessException("分组不存在");
        }
        group.setStatus(status);
        appGroupMapper.updateById(group);
    }

    private String generateGroupCode() {
        return "GRP" + System.currentTimeMillis();
    }
}
