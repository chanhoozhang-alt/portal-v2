package com.example.portal.common.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/** 用户与角色的分配关系。 */
@Data
@TableName("user_role")
public class UserRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String appCode;
    private String roleCode;
    private String userId;
    private String userName;
    private String orgId;
    private String orgName;
    private String deptId;
    private String deptName;
    /** ENABLED-启用, DISABLED-停用 */
    private String status;
    /** 授权人 ID */
    private String grantBy;
    /** 授权人姓名 */
    private String grantName;
    /** 授权时间 */
    private Date grantTime;
    /** 撤销人 ID */
    private String revokeBy;
    /** 撤销人姓名 */
    private String revokeName;
    /** 撤销时间 */
    private Date revokeTime;
}
