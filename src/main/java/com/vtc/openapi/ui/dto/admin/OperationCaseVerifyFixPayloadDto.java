package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("修复核验案件载荷")
public class OperationCaseVerifyFixPayloadDto {

    @ApiModelProperty("修复核验任务")
    private MockVerifyFixJobDto job;

    @ApiModelProperty("VTC centerSubId")
    private String centerSubId;

    @ApiModelProperty("VTC centerPlanId")
    private String centerPlanId;

    @ApiModelProperty("VTC surveyId")
    private String surveyId;

    @ApiModelProperty("扫描器类型")
    private String scannerType;

    @ApiModelProperty("复扫目标 IP")
    private String inputIps;

    @ApiModelProperty("进度 0-100")
    private Integer progress;
}
