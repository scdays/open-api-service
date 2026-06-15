package com.vtc.openapi.domain.webhook.service.business.impl;

import com.alibaba.fastjson.JSON;
import com.vtc.openapi.domain.partner.model.entity.PartnerWebhookConfigDO;
import com.vtc.openapi.domain.partner.repository.IPartnerRepository;
import com.vtc.openapi.domain.webhook.model.WebhookEvent;
import com.vtc.openapi.domain.webhook.service.business.IWebhookDomainService;
import com.vtc.openapi.infra.adapter.WebhookDeliveryAdapter;
import com.vtc.openapi.infra.dao.WebhookDeliveryLogMapper;
import com.vtc.openapi.infra.dao.po.WebhookDeliveryLogPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Webhook 出站投递实现。
 * 异步发送 HTTP POST 到 Partner callbackUrl，携带 HMAC-SHA256 验签头（文档 §6），
 * 失败重试最多 3 次（指数退避）。
 */
@Service
public class WebhookDomainServiceImpl implements IWebhookDomainService {

    private static final Logger log = LoggerFactory.getLogger(WebhookDomainServiceImpl.class);
    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"));

    private static final int MAX_RETRIES = 3;
    private static final long[] BACKOFF_MS = {1000L, 2000L, 4000L};

    private final IPartnerRepository partnerRepository;
    private final WebhookDeliveryAdapter deliveryAdapter;
    private final WebhookDeliveryLogMapper deliveryLogMapper;

    public WebhookDomainServiceImpl(IPartnerRepository partnerRepository,
                                    WebhookDeliveryAdapter deliveryAdapter,
                                    WebhookDeliveryLogMapper deliveryLogMapper) {
        this.partnerRepository = partnerRepository;
        this.deliveryAdapter = deliveryAdapter;
        this.deliveryLogMapper = deliveryLogMapper;
    }

    @Override
    @Async
    public void deliver(WebhookEvent event) {
        if (event == null || event.getPartnerId() == null) {
            return;
        }

        PartnerWebhookConfigDO config = partnerRepository.findWebhookConfig(event.getPartnerId());
        if (config == null || !StringUtils.hasText(config.getCallbackUrl())) {
            log.debug("Partner {} 未配置 callbackUrl，跳过 Webhook 投递", event.getPartnerId());
            return;
        }

        String callbackUrl = config.getCallbackUrl();
        String webhookSecret = config.getWebhookSecret();

        if (event.getEventId() == null) {
            event.setEventId("evt-" + java.util.UUID.randomUUID().toString().replace("-", ""));
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(ISO_FMT.format(Instant.now()));
        }

        String payloadJson = JSON.toJSONString(event);

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            WebhookDeliveryLogPO logEntry = createLogEntry(event, callbackUrl, payloadJson, attempt);

            try {
                int httpStatus = deliveryAdapter.post(callbackUrl, payloadJson, webhookSecret);
                logEntry.setHttpStatus(httpStatus);
                logEntry.setStatus(httpStatus >= 200 && httpStatus < 300 ? "SUCCESS" : "FAILED");
            } catch (Exception ex) {
                log.warn("Webhook 投递失败: partnerId={} eventType={} 第{}次尝试",
                        event.getPartnerId(), event.getEventType(), attempt, ex);
                logEntry.setHttpStatus(-1);
                logEntry.setStatus("FAILED");
            }

            deliveryLogMapper.insert(logEntry);

            if ("SUCCESS".equals(logEntry.getStatus())) {
                return;
            }

            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(BACKOFF_MS[Math.min(attempt, BACKOFF_MS.length - 1)]);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        log.warn("Webhook 投递耗尽重试次数: partnerId={} eventType={}",
                event.getPartnerId(), event.getEventType());
    }

    private WebhookDeliveryLogPO createLogEntry(WebhookEvent event, String callbackUrl,
                                                 String payloadJson, int attempt) {
        WebhookDeliveryLogPO po = new WebhookDeliveryLogPO();
        po.setPartnerId(event.getPartnerId());
        po.setEventType(event.getEventType());
        po.setPayloadJson(payloadJson);
        po.setCallbackUrl(callbackUrl);
        po.setRetryCount(attempt);
        po.setStatus("PENDING");
        po.setCreatedAt(new Date());
        return po;
    }
}