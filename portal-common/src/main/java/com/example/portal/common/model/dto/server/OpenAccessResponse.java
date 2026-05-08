package com.example.portal.common.model.dto.server;

import lombok.Data;

/** 开放 API 访问权限校验响应。 */
@Data
public class OpenAccessResponse {

    private String appCode;
    private String userId;
    private boolean hasAccess;
}
