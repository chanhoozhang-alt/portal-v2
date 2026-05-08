package com.example.portal.common.util;

import cn.hutool.json.JSONUtil;

/**
 * JSON 工具类，封装 Hutool JSONUtil，统一项目中的 JSON 序列化/反序列化入口。
 */
public class JsonUtils {

    public static String toJson(Object obj) {
        return JSONUtil.toJsonStr(obj);
    }

    public static <T> T toBean(String json, Class<T> clazz) {
        return JSONUtil.toBean(json, clazz);
    }

    public static <T> T toBean(String json, Class<T> clazz, boolean ignoreError) {
        return JSONUtil.toBean(json, clazz, ignoreError);
    }
}
