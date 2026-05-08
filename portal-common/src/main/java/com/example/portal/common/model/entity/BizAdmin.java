package com.example.portal.common.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/** 业务管理员，记录应用与用户的业务管理关系。 */
@Data
@TableName("biz_admin")
public class BizAdmin {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String appCode;
    private String userId;
    private String userName;
    private String orgId;
    private String orgName;
    private String deptId;
    private String deptName;
    /** APP-应用级管理, ROLE-角色级管理 */
    private String manageScope;
    /** ENABLED-启用, DISABLED-停用 */
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private String createBy;
    @TableField(fill = FieldFill.INSERT)
    private String createName;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateName;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
