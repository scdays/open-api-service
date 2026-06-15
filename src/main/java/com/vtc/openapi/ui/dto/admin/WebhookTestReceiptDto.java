package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("WebhookTestReceiptDto")
public class WebhookTestReceiptDto {

    @ApiModelProperty("Received at (UTC ISO)")
    private String receivedAt;

    @ApiModelProperty("Event ID")
    private String eventId;

    @ApiModelProperty("Event type")
    private String eventType;

    @ApiModelProperty("Partner ID")
    private String partnerId;

    @ApiModelProperty("Occurred at")
    private String occurredAt;

    @ApiModelProperty("Payload node")
    private Object body;

    @ApiModelProperty("Raw JSON body")
    private String rawBody;

    @ApiModelProperty("X-Webhook-Signature header")
    private String signature;

    @ApiModelProperty("X-Webhook-Timestamp header")
    private String timestamp;

    @ApiModelProperty("Signature valid when webhookSecret is configured")
    private Boolean signatureValid;
}
