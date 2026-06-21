package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel("修复核验内部任务")
public class MockVerifyFixJobDto {

    @ApiModelProperty("verifyFixJobId")
    private String jobId;
    private String partnerId;
    private String batchId;
    private String caseId;
    private String status;
    private Integer itemCount;
    private Integer progress;
    private Integer retryCount;
    private Integer rescanSubCount;
    private Boolean rescanImported;
    private String errorMessage;
    private String finishedAt;
    private String createdAt;
    private List<MockVerifyFixJobItemDto> items = new ArrayList<>();
}
