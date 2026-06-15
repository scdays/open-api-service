package com.vtc.openapi.domain.open.model.query;

import lombok.Data;

@Data
public class WebhookDeliveryLogQuery {

    private String partnerId;

    private String eventType;

    private String status;

    private String resourceType;

    private String resourceId;

    private int page = 1;

    private int size = 20;
}
