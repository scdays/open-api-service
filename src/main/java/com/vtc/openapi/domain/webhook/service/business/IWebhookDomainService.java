package com.vtc.openapi.domain.webhook.service.business;

import com.vtc.openapi.domain.open.model.entity.WebhookDeliveryLogDO;
import com.vtc.openapi.domain.webhook.model.WebhookEvent;

import java.util.List;

/**
 * Webhook 出站投递领域服务。
 */
public interface IWebhookDomainService {

    void deliver(WebhookEvent event);

    WebhookDeliveryLogDO requireDeliveryLog(Long deliveryLogId);

    WebhookDeliveryLogDO retryDelivery(Long deliveryLogId);

    List<WebhookDeliveryLogDO> listRelatedAttempts(WebhookDeliveryLogDO source);
}