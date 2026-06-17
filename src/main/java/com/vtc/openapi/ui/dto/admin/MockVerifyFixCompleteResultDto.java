package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import lombok.Data;

@Data
@ApiModel("修复核验完成结果")
public class MockVerifyFixCompleteResultDto {

    private String jobId;
    private String status;
    private String message;
}
