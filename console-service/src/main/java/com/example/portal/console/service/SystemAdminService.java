package com.example.portal.console.service;

import com.example.portal.common.model.common.PageResult;
import com.example.portal.common.model.entity.SystemAdmin;

import java.util.List;

/**
 * 系统管理员管理服务接口：系统管理员的增删改查和启用/停用。
 */
public interface SystemAdminService {

    PageResult<SystemAdmin> list(String userName, String userId, int pageNum, int pageSize);

    void add(List<String> userIds);

    void disable(String userId);

    void enable(String userId);

    void remove(String userId);
}
