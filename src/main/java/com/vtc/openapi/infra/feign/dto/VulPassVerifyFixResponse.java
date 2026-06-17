package com.vtc.openapi.infra.feign.dto;

import lombok.Data;

@Data
public class VulPassVerifyFixResponse {

    private String status;
    private String message;
    private String verifyFixJobId;
    private String verifyFixStatus;
    private String vulInfoID;
    private Long passTaskId;
    private Long passSubTaskId;
    private Integer currentStatus;
}
