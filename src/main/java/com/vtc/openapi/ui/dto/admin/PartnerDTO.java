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

    @ApiModelProperty("允许下载的外发阶段（逗号分隔 exportStage）；空则继承全局默认")
    private String downloadableStages;

    @ApiModelProperty("网关限流 QPS")
    private Integer rateLimitQps;

    @ApiModelProperty("是否已配置 Webhook Secret（详情不含明文）")
    private Boolean webhookSecretConfigured;

    @ApiModelProperty("Webhook Secret（仅创建或轮换时可能返回，关闭后不可再查）")
    private String webhookSecret;
}
