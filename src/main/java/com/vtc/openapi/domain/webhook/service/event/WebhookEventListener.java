package com.vtc.openapi.domain.webhook.service.event;

import com.vtc.openapi.domain.webhook.model.WebhookEvent;
import com.vtc.openapi.domain.webhook.service.business.IWebhookDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WebhookEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebhookEventListener.class);

    private final IWebhookDomainService webhookDomainService;

    public WebhookEventListener(IWebhookDomainService webhookDomainService) {
        this.webhookDomainService = webhookDomainService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onWebhookEvent(WebhookEvent event) {
        log.info("webhook event received by listener eventType={} partnerId={}",
                event != null ? event.getEventType() : null, event != null ? event.getPartnerId() : null);
        try {
            webhookDomainService.deliver(event);
        } catch (Exception ex) {
            // @Async 异步线程异常默认会被吞，此处显式记录，避免投递失败无痕
            log.error("webhook deliver async error eventType={} partnerId={}: {}",
                    event != null ? event.getEventType() : null,
                    event != null ? event.getPartnerId() : null, ex.getMessage(), ex);
        }
    }
}
