package com.example.portal.common.model.common;

import lombok.Data;

/**
 * 统一 API 响应体，所有接口返回此结构，包含 code / message / data 三字段。
 */
@Data
public class Result<T> {

    /** 状态码(200-成功, 其他-失败) */
    private int code;
    /** 提示信息 */
    private String message;
    /** 数据载荷 */
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    public static <T> Result<T> error(String message) {
        return error(500, message);
    }
}
