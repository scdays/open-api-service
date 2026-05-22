package com.vtc.openapi.ui.dto.auth;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("PartnerTokenIssueRequest")
public class PartnerTokenIssueRequest {

    @ApiModelProperty(value = "固定 client_credentials", required = true, example = "client_credentials")
    private String grantType;

    @ApiModelProperty(value = "客户端 ID", required = true)
    private String clientId;

    @ApiModelProperty(value = "客户端密钥", required = true)
    private String clientSecret;
}
