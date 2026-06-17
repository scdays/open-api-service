package com.vtc.openapi.infra.feign.dto;

import lombok.Data;

@Data
public class VulPassSubTaskProgress {

    private Long subTaskId;
    private String scannerVendor;
    private String status;
    private Integer progress;
}
