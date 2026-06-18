package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel("批量实例操作案件载荷")
public class OperationCaseBatchPayloadDto {

    @ApiModelProperty("批次 ID")
    private String batchId;

    @ApiModelProperty("成功条数")
    private int successCount;

    @ApiModelProperty("失败条数")
    private int failedCount;

    @ApiModelProperty("响应摘要 JSON（原始）")
    private String resultSummaryJson;

    @ApiModelProperty("成功实例 vulInfoID 列表")
    private List<String> successVulInfoIds = new ArrayList<>();

    @ApiModelProperty("失败实例 vulInfoID 列表")
    private List<String> failedVulInfoIds = new ArrayList<>();

    @ApiModelProperty("目标明细（W4 case_target）")
    private List<OperationCaseBatchTargetDto> targets = new ArrayList<>();
}
