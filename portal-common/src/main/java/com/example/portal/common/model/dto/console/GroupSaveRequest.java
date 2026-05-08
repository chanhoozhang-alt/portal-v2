package com.example.portal.common.model.dto.console;

import lombok.Data;

/** 应用分组新增/修改请求参数。 */
@Data
public class GroupSaveRequest {

    private String groupCode;
    private String groupName;
    private String orgId;
    private String orgName;
    /** 排序号(越小越靠前) */
    private Integer sortNo;
}
