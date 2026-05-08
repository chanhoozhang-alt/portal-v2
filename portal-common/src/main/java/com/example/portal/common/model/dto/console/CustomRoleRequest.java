package com.example.portal.common.model.dto.console;

import lombok.Data;

/** 自定义角色新增/修改/状态变更请求参数。 */
@Data
public class CustomRoleRequest {

    private String roleCode;
    private String roleName;
    private String roleDesc;
    /** ENABLED-启用, DISABLED-停用（状态变更时使用） */
    private String status;
}
