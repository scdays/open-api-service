package com.vtc.openapi.domain.task.model.vo;

import lombok.Data;

@Data
public class ScanEngineProgressResult {

    private String status;

    private Integer progress;

    private String errorMessage;
}
