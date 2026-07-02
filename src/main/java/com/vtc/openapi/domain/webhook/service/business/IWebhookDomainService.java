package com.vtc.openapi.domain.webhook.service.business;

import com.vtc.openapi.domain.open.model.entity.WebhookDeliveryLogDO;

import java.util.List;

/**
 * Webhook 出站投递历史记录领域服务。
 * <p>
 * 首次投递已迁移至 eventplus EventBus（由 platform-admin 消费投递），
 * 本接口仅保留历史投递记录的查询/手动重试能力（dormant 兼容）。
 */
public interface IWebhookDomainService {

    WebhookDeliveryLogDO requireDeliveryLog(Long deliveryLogId);

    WebhookDeliveryLogDO retryDelivery(Long deliveryLogId);

    List<WebhookDeliveryLogDO> listRelatedAttempts(WebhookDeliveryLogDO source);
}