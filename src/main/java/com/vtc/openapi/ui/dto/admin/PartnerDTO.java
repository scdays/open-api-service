package com.vtc.openapi.ui.dto.admin;

import com.botany.spore.ddd.ui.dto.BaseDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("PartnerDTO")
public class PartnerDTO extends BaseDTO {

    @ApiModelProperty("Partner 唯一标识")
    private String partnerId;

    @ApiModelProperty("名称")
    private String partnerName;

    @ApiModelProperty("类型，如 SIEM/ITSM")
    private String partnerType;

    @ApiModelProperty("状态 ACTIVE/DISABLED")
    private String status;

    @ApiModelProperty("能力码列表")
    private List<String> capabilities;

    @ApiModelProperty("默认 Webhook 回调地址")
    private String defaultCallbackUrl;

    @ApiModelProperty("网关限流 QPS")
    private Integer rateLimitQps;
}
