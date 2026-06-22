package com.vtc.openapi.ui.params.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("UpdatePartnerParams")
public class UpdatePartnerParams {

    @ApiModelProperty("名称")
    private String partnerName;

    @ApiModelProperty("状态 ACTIVE/DISABLED")
    private String status;

    @ApiModelProperty("能力码列表")
    private List<String> capabilities;

    @ApiModelProperty("默认 Webhook 回调地址")
    private String defaultCallbackUrl;

    @ApiModelProperty("允许下载的外发阶段（逗号分隔 exportStage）；空则继承全局默认")
    private String downloadableStages;

    @ApiModelProperty("网关限流 QPS")
    private Integer rateLimitQps;
}
