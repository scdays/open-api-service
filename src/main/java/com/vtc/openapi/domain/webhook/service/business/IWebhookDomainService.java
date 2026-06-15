package com.vtc.openapi.domain.webhook.service.business;

import com.vtc.openapi.domain.webhook.model.WebhookEvent;

/**
 * Webhook 出站投递领域服务。
 */
public interface IWebhookDomainService {

    /**
     * 异步投递 Webhook 事件到 Partner 的 callbackUrl。
     * 投递失败不影响调用方。
     */
    void deliver(WebhookEvent event);
}