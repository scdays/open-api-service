package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("CreatePartnerRequest")
public class CreatePartnerRequest {

    @ApiModelProperty(value = "Partner 唯一标识", required = true)
    private String partnerId;

    @ApiModelProperty(value = "名称", required = true)
    private String partnerName;

    @ApiModelProperty("类型，如 SIEM/ITSM")
    private String partnerType;

    @ApiModelProperty("能力码列表，如 TASK_READ")
    private List<String> capabilities;

    @ApiModelProperty("默认 Webhook 回调地址")
    private String defaultCallbackUrl;

    @ApiModelProperty("网关限流 QPS")
    private Integer rateLimitQps;
}
