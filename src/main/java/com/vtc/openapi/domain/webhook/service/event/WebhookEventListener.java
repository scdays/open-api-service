package com.vtc.openapi.domain.webhook.service.event;

import com.vtc.openapi.domain.webhook.model.WebhookEvent;
import com.vtc.openapi.domain.webhook.service.business.IWebhookDomainService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WebhookEventListener {

    private final IWebhookDomainService webhookDomainService;

    public WebhookEventListener(IWebhookDomainService webhookDomainService) {
        this.webhookDomainService = webhookDomainService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onWebhookEvent(WebhookEvent event) {
        webhookDomainService.deliver(event);
    }
}
