package com.vtc.openapi.domain.instance.model.result;

import lombok.Data;

/**
 * 实例状态变更结果。
 */
@Data
public class InstanceStateResult {
    private String vulInfoId;
    private Integer vulInfoStat;
    private Integer lvRsn;
    private String transferTime;
    private Integer srcMethod;
    private String remedDesc;
    private String archiveReason;
    private String verifyFixJobId;
    private String verifyFixStatus;
    private String message;
}
