package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("WebhookTestInboxPageDto")
public class WebhookTestInboxPageDto {

    @ApiModelProperty("Matched entry count")
    private long total;

    @ApiModelProperty("Inbox entries")
    private List<WebhookTestReceiptDto> items;

    @ApiModelProperty("Callback URL for Partner defaultCallbackUrl")
    private String callbackUrl;
}
