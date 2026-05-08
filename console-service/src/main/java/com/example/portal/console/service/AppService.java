package com.example.portal.console.service;

import com.example.portal.common.model.common.PageResult;
import com.example.portal.common.model.dto.console.AppSaveRequest;
import com.example.portal.common.model.entity.AppInfo;

/**
 * 应用管理服务接口：应用列表、新增/修改、启用/停用、重置密钥。
 */
public interface AppService {

    PageResult<AppInfo> list(String appCode, String appName, String status, int pageNum, int pageSize);

    AppInfo getByAppCode(String appCode);

    void save(AppSaveRequest request);

    void disable(String appCode);

    void enable(String appCode);

    String resetClientSecret(String appCode);
}
