package com.example.portal.portal.service.impl;

import com.example.portal.common.cache.PermissionCacheManager;
import com.example.portal.common.context.UserContext;
import com.example.portal.common.exception.ForbiddenException;
import com.example.portal.common.model.common.AppBrief;
import com.example.portal.common.model.dto.portal.PortalInitResponse;
import com.example.portal.common.model.dto.server.AuthInitResponse;
import com.example.portal.common.model.common.Result;
import com.example.portal.common.util.JsonUtils;
import com.example.portal.portal.feign.ServerFeignClient;
import com.example.portal.portal.service.PortalService;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 门户首页服务实现，组装用户可见应用并按组分页返回。
 * 提供给 portal 前端展示用户个人信息、管理员标识和可访问应用列表。
 *
 * <p>核心职责：
 * <ul>
 *   <li>从 Redis 读取可见应用列表，按 groupCode 分组并排序</li>
 *   <li>组装用户基础信息（姓名、组织、管理员权限）</li>
 *   <li>按 appCode 查询跳转信息（含权限校验）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalServiceImpl implements PortalService {

    private final PermissionCacheManager cacheManager;
    private final ServerFeignClient serverFeignClient;

    @Override
    public PortalInitResponse init() {
        UserContext ctx = UserContext.get();
        if (ctx == null) {
            throw new ForbiddenException("未认证");
        }

        PortalInitResponse resp = buildBaseResponse(ctx);

        // 从 Redis 读取可见应用，应已由 server-service 的 AuthInterceptor 预写入
        String appsJson = cacheManager.getVisibleApps(ctx.getUserId());
        List<AuthInitResponse.VisibleApp> visibleApps;
        if (appsJson != null) {
            visibleApps = JsonUtils.toBean(appsJson, List.class) != null
                    ? parseVisibleApps(appsJson) : Collections.emptyList();
        } else {
            // 缓存未命中不在此处理（应由拦截器触发），返回空
            visibleApps = Collections.emptyList();
        }

        resp.setVisibleApps(groupApps(visibleApps));
        return resp;
    }

    @Override
    public PortalInitResponse apps() {
        return init();
    }

    @Override
    public PortalInitResponse adminPermission() {
        UserContext ctx = UserContext.get();
        if (ctx == null) {
            throw new ForbiddenException("未认证");
        }
        return buildBaseResponse(ctx);
    }

    @Override
    public PortalInitResponse.AppItem jumpInfo(String appCode) {
        UserContext ctx = UserContext.get();
        if (ctx == null) {
            throw new ForbiddenException("未认证");
        }

        // 再次读取可见应用列表，确认用户对该 appCode 有权限（即使 init 阶段已校验过）
        String appsJson = cacheManager.getVisibleApps(ctx.getUserId());
        if (appsJson == null) {
            throw new ForbiddenException("无权访问该应用");
        }

        List<AuthInitResponse.VisibleApp> apps = parseVisibleApps(appsJson);
        return apps.stream()
                .filter(a -> a.getAppCode().equals(appCode))
                .findFirst()
                .map(a -> {
                    PortalInitResponse.AppItem item = new PortalInitResponse.AppItem();
                    item.setAppCode(a.getAppCode());
                    item.setAppName(a.getAppName());
                    item.setAppIcon(a.getAppIcon());
                    item.setAppDesc(a.getAppDesc());
                    item.setJumpUrl(a.getJumpUrl());
                    item.setShowMenu(a.getShowMenu());
                    item.setShowHeader(a.getShowHeader());
                    item.setEnableWatermark(a.getEnableWatermark());
                    item.setVisibleType(a.getVisibleType());
                    return item;
                })
                .orElseThrow(() -> new ForbiddenException("无权访问该应用"));
    }

    private PortalInitResponse buildBaseResponse(UserContext ctx) {
        PortalInitResponse resp = new PortalInitResponse();
        PortalInitResponse.UserInfo user = new PortalInitResponse.UserInfo();
        user.setUserId(ctx.getUserId());
        user.setUserName(ctx.getUserName());
        user.setOrgName(ctx.getOrgName());
        user.setDeptName(ctx.getDeptName());
        resp.setUser(user);
        resp.setAdmin(ctx.isAdmin());
        resp.setSystemAdmin(ctx.isSystemAdmin());
        resp.setAppAdminApps(ctx.getAppAdminApps());
        resp.setBizAdminApps(ctx.getBizAdminApps());
        return resp;
    }

    /**
     * 将可见应用列表按 groupCode 分组，每组内按 appSortNo 排序，组间按 groupSortNo 排序。
     * 无分组的应用归入 "other"（显示为"其他应用"）。
     */
    private List<PortalInitResponse.AppGroup> groupApps(List<AuthInitResponse.VisibleApp> apps) {
        Map<String, List<AuthInitResponse.VisibleApp>> grouped = new LinkedHashMap<>();
        Map<String, String> groupNames = new HashMap<>();
        Map<String, Integer> groupSortNos = new HashMap<>();

        for (AuthInitResponse.VisibleApp app : apps) {
            String groupCode = app.getGroupCode() != null ? app.getGroupCode() : "other";
            groupNames.putIfAbsent(groupCode, app.getGroupName() != null ? app.getGroupName() : "其他应用");
            if (app.getGroupSortNo() != null) {
                groupSortNos.putIfAbsent(groupCode, app.getGroupSortNo());
            }
            grouped.computeIfAbsent(groupCode, k -> new ArrayList<>()).add(app);
        }

        return grouped.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> groupSortNos.getOrDefault(e.getKey(), Integer.MAX_VALUE)))
                .map(e -> {
                    PortalInitResponse.AppGroup g = new PortalInitResponse.AppGroup();
                    g.setGroupCode(e.getKey());
                    g.setGroupName(groupNames.get(e.getKey()));
                    g.setGroupSortNo(groupSortNos.get(e.getKey()));
                    g.setApps(e.getValue().stream().map(a -> {
                        PortalInitResponse.AppItem item = new PortalInitResponse.AppItem();
                        item.setAppCode(a.getAppCode());
                        item.setAppName(a.getAppName());
                        item.setAppIcon(a.getAppIcon());
                        item.setAppDesc(a.getAppDesc());
                        item.setJumpUrl(a.getJumpUrl());
                        item.setVisibleType(a.getVisibleType());
                        item.setShowMenu(a.getShowMenu());
                        item.setShowHeader(a.getShowHeader());
                        item.setEnableWatermark(a.getEnableWatermark());
                        item.setAppSortNo(a.getAppSortNo());
                        return item;
                    }).collect(Collectors.toList()));
                    return g;
                })
                .collect(Collectors.toList());
    }

    /**
     * 将 Redis 中缓存的 JSON 数组反序列化为 VisibleApp 对象列表。
     * 因泛型擦除无法直接反序列化泛型 List，需要手动逐字段映射。
     */
    private List<AuthInitResponse.VisibleApp> parseVisibleApps(String json) {
        JSONArray arr = JsonUtils.toBean(json, JSONArray.class);
        if (arr == null) {
            return Collections.emptyList();
        }
        List<AuthInitResponse.VisibleApp> result = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            AuthInitResponse.VisibleApp app = new AuthInitResponse.VisibleApp();
            app.setAppCode(obj.getStr("appCode"));
            app.setAppName(obj.getStr("appName"));
            app.setAppIcon(obj.getStr("appIcon"));
            app.setAppDesc(obj.getStr("appDesc"));
            app.setJumpUrl(obj.getStr("jumpUrl"));
            app.setVisibleType(obj.getStr("visibleType"));
            app.setShowMenu(obj.getBool("showMenu", false));
            app.setShowHeader(obj.getBool("showHeader", false));
            app.setEnableWatermark(obj.getBool("enableWatermark", false));
            app.setGroupCode(obj.getStr("groupCode"));
            app.setGroupName(obj.getStr("groupName"));
            app.setGroupSortNo(obj.getInt("groupSortNo", 0));
            app.setAppSortNo(obj.getInt("appSortNo", 0));
            result.add(app);
        }
        return result;
    }
}
