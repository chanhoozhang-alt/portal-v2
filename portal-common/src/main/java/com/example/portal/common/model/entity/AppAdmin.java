package com.example.portal.common.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/** 应用管理员，记录应用与用户的 admin 关系。 */
@Data
@TableName("app_admin")
public class AppAdmin {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String appCode;
    private String userId;
    private String userName;
    private String orgId;
    private String orgName;
    private String deptId;
    private String deptName;
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
