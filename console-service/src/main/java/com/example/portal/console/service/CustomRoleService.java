package com.example.portal.console.service;

import com.example.portal.common.model.entity.AppCustomRole;
import java.util.List;

/**
 * 自定义角色管理服务接口：角色的增删改查和启用/停用。
 */
public interface CustomRoleService {
    List<AppCustomRole> list(String appCode, String roleCode, String roleName, String status);
    void add(String appCode, String roleCode, String roleName, String roleDesc);
    void update(String appCode, String roleCode, String roleName, String roleDesc);
    void disable(String appCode, String roleCode);
    void enable(String appCode, String roleCode);
}
