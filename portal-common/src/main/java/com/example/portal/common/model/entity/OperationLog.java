package com.example.portal.common.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/** 操作日志，记录管理后台用户的关键操作。 */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属模块 */
    private String moduleName;
    /** 操作类型(ADD/DELETE/UPDATE/...) */
    private String operationType;
    /** 操作描述 */
    private String operationDesc;
    /** 操作对象类型 */
    private String targetType;
    /** 操作对象 ID */
    private String targetId;
    /** 操作人 ID */
    private String operatorId;
    /** 操作人姓名 */
    private String operatorName;
    /** 请求 URI */
    private String requestUri;
    /** 请求方法(GET/POST/...) */
    private String requestMethod;
    /** 请求参数 */
    private String requestParam;
    /** SUCCESS-成功, FAIL-失败 */
    private String resultStatus;
    /** 错误信息(仅失败时记录) */
    private String errorMsg;
    /** 操作时间 */
    private Date operationTime;
}
