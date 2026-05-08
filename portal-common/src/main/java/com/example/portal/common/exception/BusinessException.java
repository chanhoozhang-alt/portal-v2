package com.example.portal.common.exception;

/** 业务异常，返回 500 给前端展示错误信息。 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
