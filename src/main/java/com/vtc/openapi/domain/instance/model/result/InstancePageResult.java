package com.vtc.openapi.domain.instance.model.result;

import java.util.List;
import lombok.Data;

/**
 * 实例分页结果。
 */
@Data
public class InstancePageResult {
    private Integer page;
    private Integer size;
    private Long total;
    private List<InstanceItemResult> items;
}