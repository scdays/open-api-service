package com.vtc.openapi.infra.feign.dto;

import lombok.Data;

@Data
public class VulPassSubTaskCreated {

    private Long subTaskId;
    private String scannerVendor;
    private String externalSurveyId;
}
