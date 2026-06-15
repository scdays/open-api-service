package com.vtc.openapi.app.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.app.service.IWebhookTestAppService;
import com.vtc.openapi.domain.partner.model.entity.PartnerWebhookConfigDO;
import com.vtc.openapi.domain.partner.repository.IPartnerRepository;
import com.vtc.openapi.infra.adapter.WebhookDeliveryAdapter;
import com.vtc.openapi.infra.dev.WebhookTestInbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WebhookTestAppServiceImpl implements IWebhookTestAppService {

    private static final Logger log = LoggerFactory.getLogger(WebhookTestAppServiceImpl.class);

    private final WebhookTestInbox inbox;
    private final IPartnerRepository partnerRepository;

    public WebhookTestAppServiceImpl(WebhookTestInbox inbox, IPartnerRepository partnerRepository) {
        this.inbox = inbox;
        this.partnerRepository = partnerRepository;
    }

    @Override
    public void receive(String rawBody, String signature, String timestamp) {
        WebhookTestInbox.WebhookTestReceipt receipt =
                WebhookTestInbox.newReceipt(rawBody, signature, timestamp);

        try {
            JSONObject envelope = JSON.parseObject(rawBody);
            if (envelope != null) {
                receipt.setEventId(envelope.getString("eventId"));
                receipt.setEventType(envelope.getString("eventType"));
                receipt.setPartnerId(envelope.getString("partnerId"));
                receipt.setOccurredAt(envelope.getString("occurredAt"));
                receipt.setBody(envelope.get("payload"));
                verifySignature(receipt, rawBody, signature);
            }
        } catch (Exception ex) {
            log.debug("webhook test receiver failed to parse JSON: {}", ex.getMessage());
        }

        inbox.add(receipt);
        log.info("webhook test received: eventType={} partnerId={} eventId={}",
                receipt.getEventType(), receipt.getPartnerId(), receipt.getEventId());
    }

    private void verifySignature(WebhookTestInbox.WebhookTestReceipt receipt, String rawBody, String signature) {
        if (!StringUtils.hasText(signature) || !StringUtils.hasText(receipt.getPartnerId())) {
            return;
        }
        PartnerWebhookConfigDO config = partnerRepository.findWebhookConfig(receipt.getPartnerId());
        if (config == null || !StringUtils.hasText(config.getWebhookSecret())) {
            return;
        }
        String expected = WebhookDeliveryAdapter.hmacSha256Hex(config.getWebhookSecret(), rawBody);
        receipt.setSignatureValid(expected.equalsIgnoreCase(signature));
    }
}
