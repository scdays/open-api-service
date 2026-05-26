package com.vtc.openapi.ui.dto.admin;

import com.botany.spore.ddd.ui.dto.BaseDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("PartnerCredentialDTO")
public class PartnerCredentialDTO extends BaseDTO {

    @ApiModelProperty("Partner ID")
    private String partnerId;

    @ApiModelProperty("客户端 ID")
    private String clientId;

    @ApiModelProperty("客户端密钥（仅创建时返回明文）")
    private String clientSecret;

    @ApiModelProperty("状态 ACTIVE/DISABLED")
    private String status;

    @ApiModelProperty("过期时间（可选）")
    private Date expiresAt;

    @ApiModelProperty("创建时间")
    private Date createdAt;
}
