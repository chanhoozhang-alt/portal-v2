package com.example.portal.common.model.dto.console;

import com.example.portal.common.model.entity.AppGroup;
import com.example.portal.common.model.entity.AppGroupRelation;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 应用分组视图对象，用于分组列表接口返回。
 * 包含分组基本信息和该分组下的应用列表。
 */
@Data
public class AppGroupVO {

    private Long id;
    private String groupCode;
    private String groupName;
    private String orgId;
    private String orgName;
    private Integer sortNo;
    private String status;
    private String createBy;
    private String createName;
    private Date createTime;
    private String updateBy;
    private String updateName;
    private Date updateTime;
    /** 该分组下的应用列表 */
    private List<GroupAppItem> apps;

    @Data
    public static class GroupAppItem {
        private String appCode;
        private String appName;
        private Integer sortNo;
    }

    public static AppGroupVO from(AppGroup group) {
        if (group == null) {
            return null;
        }
        AppGroupVO vo = new AppGroupVO();
        vo.setId(group.getId());
        vo.setGroupCode(group.getGroupCode());
        vo.setGroupName(group.getGroupName());
        vo.setOrgId(group.getOrgId());
        vo.setOrgName(group.getOrgName());
        vo.setSortNo(group.getSortNo());
        vo.setStatus(group.getStatus());
        vo.setCreateBy(group.getCreateBy());
        vo.setCreateName(group.getCreateName());
        vo.setCreateTime(group.getCreateTime());
        vo.setUpdateBy(group.getUpdateBy());
        vo.setUpdateName(group.getUpdateName());
        vo.setUpdateTime(group.getUpdateTime());
        vo.setApps(new ArrayList<>());
        return vo;
    }
}
