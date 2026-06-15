package com.vtc.openapi.domain.webhook.service.event;

import com.vtc.openapi.domain.webhook.model.WebhookEvent;
import com.vtc.openapi.domain.webhook.service.business.IWebhookDomainService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class WebhookEventListener {

    private final IWebhookDomainService webhookDomainService;

    public WebhookEventListener(IWebhookDomainService webhookDomainService) {
        this.webhookDomainService = webhookDomainService;
    }

    @Async
    @EventListener
    public void onWebhookEvent(WebhookEvent event) {
        webhookDomainService.deliver(event);
    }
}
