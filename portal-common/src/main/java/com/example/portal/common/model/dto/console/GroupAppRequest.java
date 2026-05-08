package com.example.portal.common.model.dto.console;

import lombok.Data;

import java.util.List;

/** 分组绑定应用请求参数。 */
@Data
public class GroupAppRequest {

    private List<String> appCodes;
}
