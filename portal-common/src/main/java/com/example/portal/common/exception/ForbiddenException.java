package com.example.portal.common.exception;

/** 权限异常，返回 403。 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
