package com.example.portal.console.service;

import com.example.portal.common.model.dto.console.AppGroupVO;
import com.example.portal.common.model.dto.console.GroupSaveRequest;
import com.example.portal.common.model.dto.console.GroupSortRequest;

import java.util.List;

/**
 * 应用分组管理服务接口：分组的增删改查，及应用与分组的绑定/解绑/排序。
 */
public interface AppGroupService {

    List<AppGroupVO> list();

    void add(GroupSaveRequest request);

    void update(String groupCode, GroupSaveRequest request);

    void disable(String groupCode);

    void enable(String groupCode);

    void bindApps(String groupCode, List<String> appCodes);

    void unbindApp(String groupCode, String appCode);

    void sortApps(String groupCode, GroupSortRequest request);
}
