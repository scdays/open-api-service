package com.vtc.openapi.domain.open.model.entity;

import com.botany.spore.ddd.domain.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class WebhookDeliveryLogDO extends BaseDO {

    private Long id;

    private String partnerId;

    private String eventType;

    private String eventId;

    private String resourceType;

    private String resourceId;

    private String resourceIdsJson;

    private String triggerSource;

    private String payloadJson;

    private String callbackUrl;

    private Integer httpStatus;

    private Integer retryCount;

    private String status;

    private Date createdAt;

    private Date nextRetryAt;
}
