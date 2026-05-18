package com.example.portal.common.model.dto.auth;

import lombok.Data;

/** 上游授权码登录前保存的临时 state，用于防伪和登录后回跳。 */
@Data
public class AuthState {

    private String state;
    private String redirectAfterLogin;
    private Long createdAt;
}
