package com.example.portal.common.exception;

/** 未认证异常，返回 401。 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
