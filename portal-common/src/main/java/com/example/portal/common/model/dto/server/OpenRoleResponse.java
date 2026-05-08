package com.example.portal.common.model.dto.server;

import lombok.Data;

import java.util.List;

/** 开放 API 用户角色查询响应。 */
@Data
public class OpenRoleResponse {

    private String appCode;
    private String userId;
    private List<RoleItem> roles;

    @Data
    public static class RoleItem {
        private String roleCode;
        private String roleName;
    }
}
