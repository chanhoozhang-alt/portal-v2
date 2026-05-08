package com.example.portal.common.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/** 应用分组。 */
@Data
@TableName("app_group")
public class AppGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String groupCode;
    private String groupName;
    private String orgId;
    private String orgName;
    /** 排序号 */
    private Integer sortNo;
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
