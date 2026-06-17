package com.vtc.openapi.ui.dto.internal;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("verify-fix 完成回调（vul-pass → open-api）")
public class VerifyFixCompletedNotifyRequest {

    @ApiModelProperty(value = "Partner ID", required = true)
    private String partnerId;

    @ApiModelProperty(value = "漏洞实例 ID", required = true)
    private String vulInfoId;

    @ApiModelProperty(value = "任务状态", required = true, allowableValues = "FINISHED,FAILED")
    private String status;

    @ApiModelProperty(value = "终态 vulInfoStat（6/7/10）")
    private Integer resultStat;

    @ApiModelProperty(value = "受理前状态，默认 5")
    private Integer previousStat;

    @ApiModelProperty(value = "批量核验 batchId")
    private String batchId;

    @ApiModelProperty(value = "失败原因")
    private String errorMessage;
}
