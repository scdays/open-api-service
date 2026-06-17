package com.vtc.openapi.ui.dto.admin;

import com.botany.spore.ddd.ui.dto.BaseDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("PartnerWebhookSecretDTO")
public class PartnerWebhookSecretDTO extends BaseDTO {

    @ApiModelProperty("Partner 唯一标识")
    private String partnerId;

    @ApiModelProperty("Webhook Secret（仅本次返回，用于 HMAC-SHA256 验签）")
    private String webhookSecret;
}
