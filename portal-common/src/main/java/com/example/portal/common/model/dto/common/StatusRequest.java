package com.example.portal.common.model.dto.common;

import lombok.Data;

/** 状态变更请求参数。 */
@Data
public class StatusRequest {

    /** ENABLED-启用, DISABLED-停用 */
    private String status;
}
