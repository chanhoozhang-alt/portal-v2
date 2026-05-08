package com.example.portal.common.model.common;

import lombok.Data;

/**
 * 通用分页请求参数，默认第 1 页每页 10 条。
 */
@Data
public class PageRequest {

    /** 当前页码(从 1 开始) */
    private int pageNum = 1;
    /** 每页条数 */
    private int pageSize = 10;
}
