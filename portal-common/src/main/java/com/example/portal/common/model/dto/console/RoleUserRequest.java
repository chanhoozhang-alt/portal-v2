package com.example.portal.common.model.dto.console;

import lombok.Data;

import java.util.List;

/** 角色人员分配/状态变更请求参数。 */
@Data
public class RoleUserRequest {

    /** 角色编码 */
    private String roleCode;
    /** 用户 ID 列表（批量操作场景） */
    private List<String> userIds;
    /** ENABLED-启用, DISABLED-停用（状态变更时使用） */
    private String status;
}
