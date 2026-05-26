package com.vtc.openapi.ui.dto.admin;

import com.botany.spore.ddd.ui.dto.BaseDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("PartnerQuotaDTO")
public class PartnerQuotaDTO extends BaseDTO {

    @ApiModelProperty("Partner ID")
    private String partnerId;

    @ApiModelProperty("Partner 名称")
    private String partnerName;

    @ApiModelProperty("限流 QPS")
    private Integer rateLimitQps;

    @ApiModelProperty("调用总量")
    private long totalInvocations;

    @ApiModelProperty("成功量")
    private long successInvocations;

    @ApiModelProperty("失败量")
    private long failedInvocations;

    @ApiModelProperty("成功率（0~1）")
    private double successRate;
}
