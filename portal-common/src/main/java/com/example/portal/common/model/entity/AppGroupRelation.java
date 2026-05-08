package com.example.portal.common.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/** 应用与分组的关联关系。 */
@Data
@TableName("app_group_relation")
public class AppGroupRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String groupCode;
    private String appCode;
    /** 分组内排序号 */
    private Integer sortNo;
    @TableField(fill = FieldFill.INSERT)
    private String createBy;
    @TableField(fill = FieldFill.INSERT)
    private String createName;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
