package com.vtc.openapi.infra.feign.dto;

import lombok.Data;

@Data
public class VulPassCreateOpenTaskRequest {

    private String partnerId;
    private String platformTaskId;
    private String extTaskId;
    private String taskName;
    private Integer vulnType;
    private VulPassOpenScanTargets targets;
    private Integer scanTemplateId;
    private Integer reportTemplateId;
    private String scanPolicy;
    private Integer srcMethod;
    private String callbackUrl;
}
