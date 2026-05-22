package com.vtc.openapi.ui.dto.auth;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("PartnerTokenIssueResponse")
public class PartnerTokenIssueResponse {

    @ApiModelProperty("访问令牌")
    private String accessToken;

    @ApiModelProperty(value = "令牌类型", example = "Bearer")
    private String tokenType;

    @ApiModelProperty(value = "有效秒数", example = "86400")
    private Integer expiresIn;

    @ApiModelProperty("Partner ID")
    private String partnerId;
}
