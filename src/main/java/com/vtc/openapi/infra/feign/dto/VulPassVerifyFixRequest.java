package com.vtc.openapi.infra.feign.dto;

import lombok.Data;

@Data
public class VulPassVerifyFixRequest {

    private String partnerId;
    private String remark;
    private String transferTime;
}
