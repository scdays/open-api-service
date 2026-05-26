package com.vtc.openapi.ui.params.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
@ApiModel("CreatePartnerParams")
public class CreatePartnerParams {

    @NotBlank(message = "partnerId 不能为空")
    @ApiModelProperty(value = "Partner 唯一标识", required = true)
    private String partnerId;

    @NotBlank(message = "partnerName 不能为空")
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
