package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("实例操作案件载荷")
public class OperationCaseInstancePayloadDto {

    @ApiModelProperty("vulInfoID")
    private String vulInfoId;

    @ApiModelProperty("当前状态")
    private Integer vulInfoStat;

    @ApiModelProperty("关联任务 ID")
    private String taskId;

    @ApiModelProperty("漏洞名称")
    private String vulName;

    @ApiModelProperty("网络地址")
    private String vulNetAddr;

    @ApiModelProperty("受理 operationId")
    private String operationId;

    @ApiModelProperty("请求摘要 JSON")
    private String requestSummaryJson;

    @ApiModelProperty("响应摘要 JSON")
    private String resultSummaryJson;
}
