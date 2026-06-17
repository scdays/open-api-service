package com.vtc.openapi.ui.dto.open.instance;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 实例写操作响应（§5.3.1 / §5.4.1 / §5.5.1）。
 */
@Data
@ApiModel(description = "实例操作响应")
public class InstanceOperationResponse {

    @ApiModelProperty(value = "实例 ID", required = true)
    private String vulInfoID;
    @ApiModelProperty(value = "变更后状态（或核验受理时通常为 5）", required = true)
    private Integer vulInfoStat;
    @ApiModelProperty(value = "未修复原因")
    private Integer lvRsn;
    @ApiModelProperty(value = "状态变更时间")
    private String transferTime;
    @ApiModelProperty(value = "处置方式")
    private Integer srcMethod;
    @ApiModelProperty(value = "修复方案说明")
    private String remedDesc;
    @ApiModelProperty(value = "企业内部备案说明")
    private String archiveReason;
    @ApiModelProperty(value = "修复核验状态：PENDING / RUNNING")
    private String verifyFixStatus;
    @ApiModelProperty(value = "修复核验任务 ID")
    private String verifyFixJobId;
    @ApiModelProperty(value = "受理说明")
    private String message;
}
