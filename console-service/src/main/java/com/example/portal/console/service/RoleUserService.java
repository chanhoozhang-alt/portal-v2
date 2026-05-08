package com.example.portal.console.service;

import com.example.portal.common.model.common.PageResult;
import com.example.portal.common.model.entity.UserRole;

import java.util.List;
import java.util.Map;

/**
 * 用户角色分配管理服务接口：为用户分配/移除自定义角色，以及业务管理员查询管辖角色。
 */
public interface RoleUserService {

    PageResult<UserRole> listUsers(String appCode, String roleCode, String userName, String userId, String status, int pageNum, int pageSize);

    void addUsers(String appCode, String roleCode, List<String> userIds);

    void disableUser(String appCode, String roleCode, String userId);

    void enableUser(String appCode, String roleCode, String userId);

    void batchEnableUsers(String appCode, String roleCode, List<String> userIds);

    void batchDisableUsers(String appCode, String roleCode, List<String> userIds);

    void batchRemoveUsers(String appCode, String roleCode, List<String> userIds);

    List<Map<String, Object>> listBizManageRoles();
}
