package com.example.portal.common.model.common;

import lombok.Data;

import java.util.List;

/**
 * 通用分页结果，包含总记录数和当前页数据列表。
 */
@Data
public class PageResult<T> {

    /** 总记录数 */
    private long total;
    /** 当前页数据 */
    private List<T> list;

    public PageResult(long total, List<T> list) {
        this.total = total;
        this.list = list;
    }
}
