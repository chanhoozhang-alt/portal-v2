package com.example.portal.common.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/** 业务管理员角色范围。 */
@Data
@TableName("biz_admin_role_scope")
public class BizAdminRoleScope {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务管理员记录 ID */
    private Long bizAdminId;
    private String appCode;
    private String roleCode;
    /** ENABLED-启用, DISABLED-停用 */
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private String createBy;
    @TableField(fill = FieldFill.INSERT)
    private String createName;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
