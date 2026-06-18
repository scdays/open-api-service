package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("修复核验外发摘要")
public class VerifyFixExportBriefDto {

    @ApiModelProperty("exportId")
    private String exportId;

    @ApiModelProperty("taskId")
    private String taskId;

    @ApiModelProperty("exportStage")
    private String exportStage;

    @ApiModelProperty("format")
    private String format;

    @ApiModelProperty("status")
    private String status;

    @ApiModelProperty("downloadUrl")
    private String downloadUrl;

    @ApiModelProperty("generatedAt")
    private String generatedAt;
}
