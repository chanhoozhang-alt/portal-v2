package com.example.portal.common.model.dto.console;

import lombok.Data;

import java.util.List;

/** 分组内应用排序请求参数。 */
@Data
public class GroupSortRequest {

    /** 排序项列表 */
    private List<SortItem> sortItems;
    @Data
    public static class SortItem {
        private String appCode;
        /** 排序号 */
        private Integer sortNo;
    }
}
