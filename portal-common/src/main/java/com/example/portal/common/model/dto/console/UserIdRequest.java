package com.example.portal.common.model.dto.console;

import lombok.Data;

import java.util.List;

/** 用户 ID 列表请求参数（批量操作场景）。 */
@Data
public class UserIdRequest {

    private List<String> userIds;
}
