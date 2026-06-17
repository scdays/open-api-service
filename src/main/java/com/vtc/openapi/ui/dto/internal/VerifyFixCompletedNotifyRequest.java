package com.vtc.openapi.ui.dto.internal;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("verify-fix 完成回调（vul-pass → open-api）")
public class VerifyFixCompletedNotifyRequest {

    @ApiModelProperty("Partner ID")
    private String partnerId;
    @ApiModelProperty("目标 vulInfoID")
    private String vulInfoId;
    @ApiModelProperty("受理前状态，默认 5")
    private Integer previousStat;
    @ApiModelProperty("完成后状态 6/7/10")
    private Integer resultStat;
    @ApiModelProperty("批量 batchId")
    private String batchId;
    @ApiModelProperty("FINISHED / FAILED")
    private String status;
}
