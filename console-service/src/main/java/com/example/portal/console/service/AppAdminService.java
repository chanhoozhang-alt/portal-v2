package com.example.portal.console.service;

import com.example.portal.common.model.common.PageResult;
import com.example.portal.common.model.entity.AppAdmin;

import java.util.List;

/**
 * 应用管理员管理服务接口：应用管理员的增删改查和启用/停用。
 */
public interface AppAdminService {
    PageResult<AppAdmin> list(String appCode, int pageNum, int pageSize);
    void add(String appCode, List<String> userIds);
    void disable(String appCode, String userId);
    void enable(String appCode, String userId);
    void remove(String appCode, String userId);
    void removeBatch(String appCode, List<String> userIds);
}
