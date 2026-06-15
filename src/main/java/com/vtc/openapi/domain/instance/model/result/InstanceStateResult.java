package com.vtc.openapi.domain.instance.model.result;

import lombok.Data;

/**
 * 实例状态变更结果。
 */
@Data
public class InstanceStateResult {
    private String vulInfoId;
    private Integer previousStat;
    private Integer currentStat;
}