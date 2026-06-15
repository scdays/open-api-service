package com.vtc.openapi.domain.webhook.service.business.impl;

import com.alibaba.fastjson.JSON;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.open.model.entity.WebhookDeliveryLogDO;
import com.vtc.openapi.domain.open.model.support.WebhookDeliverySupport;
import com.vtc.openapi.domain.open.model.support.WebhookDeliverySupport.ResourceBinding;
import com.vtc.openapi.domain.open.repository.IApiInvocationRepository;
import com.vtc.openapi.domain.partner.model.entity.PartnerWebhookConfigDO;
import com.vtc.openapi.domain.partner.repository.IPartnerRepository;
import com.vtc.openapi.domain.webhook.model.WebhookEvent;
import com.vtc.openapi.domain.webhook.service.business.IWebhookDomainService;
import com.vtc.openapi.infra.adapter.WebhookDeliveryAdapter;
import com.vtc.openapi.infra.dao.WebhookDeliveryLogMapper;
import com.vtc.openapi.infra.dao.po.WebhookDeliveryLogPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
    private final IApiInvocationRepository apiInvocationRepository;
    private final WebhookDeliveryAdapter deliveryAdapter;
    private final WebhookDeliveryLogMapper deliveryLogMapper;

    public WebhookDomainServiceImpl(IPartnerRepository partnerRepository,
                                    IApiInvocationRepository apiInvocationRepository,
                                    WebhookDeliveryAdapter deliveryAdapter,
                                    WebhookDeliveryLogMapper deliveryLogMapper) {
        this.partnerRepository = partnerRepository;
        this.apiInvocationRepository = apiInvocationRepository;
        this.deliveryAdapter = deliveryAdapter;
        this.deliveryLogMapper = deliveryLogMapper;
    }

    @Override
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
        if (event.getOccurredAt() == null) {
            event.setOccurredAt(ISO_FMT.format(Instant.now()));
        }

        Map<String, Object> envelope = new java.util.LinkedHashMap<>();
        envelope.put("eventId", event.getEventId());
        envelope.put("eventType", event.getEventType());
        envelope.put("occurredAt", event.getOccurredAt());
        envelope.put("partnerId", event.getPartnerId());
        envelope.put("payload", event.getPayload());

        String payloadJson = JSON.toJSONString(envelope);

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

    @Override
    public WebhookDeliveryLogDO requireDeliveryLog(Long deliveryLogId) {
        if (deliveryLogId == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "deliveryId 不能为空");
        }
        WebhookDeliveryLogDO found = apiInvocationRepository.findWebhookDeliveryById(deliveryLogId);
        if (found == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "Webhook 投递记录不存在");
        }
        return found;
    }

    @Override
    public WebhookDeliveryLogDO retryDelivery(Long deliveryLogId) {
        WebhookDeliveryLogDO source = requireDeliveryLog(deliveryLogId);
        if (!StringUtils.hasText(source.getPayloadJson())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "缺少 payload，无法重试");
        }

        String callbackUrl = source.getCallbackUrl();
        String webhookSecret = null;
        PartnerWebhookConfigDO config = partnerRepository.findWebhookConfig(source.getPartnerId());
        if (config != null) {
            webhookSecret = config.getWebhookSecret();
            if (!StringUtils.hasText(callbackUrl)) {
                callbackUrl = config.getCallbackUrl();
            }
        }
        if (!StringUtils.hasText(callbackUrl)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "callbackUrl 未配置，无法重试");
        }

        WebhookDeliveryLogPO logEntry = new WebhookDeliveryLogPO();
        logEntry.setPartnerId(source.getPartnerId());
        logEntry.setEventType(source.getEventType());
        logEntry.setEventId(source.getEventId());
        logEntry.setResourceType(source.getResourceType());
        logEntry.setResourceId(source.getResourceId());
        logEntry.setResourceIdsJson(source.getResourceIdsJson());
        logEntry.setPayloadJson(source.getPayloadJson());
        logEntry.setCallbackUrl(callbackUrl);
        logEntry.setRetryCount(source.getRetryCount() == null ? 1 : source.getRetryCount() + 1);
        logEntry.setTriggerSource(WebhookDeliverySupport.TRIGGER_MANUAL_RETRY);
        logEntry.setStatus("PENDING");
        logEntry.setCreatedAt(new Date());

        try {
            int httpStatus = deliveryAdapter.post(callbackUrl, source.getPayloadJson(), webhookSecret);
            logEntry.setHttpStatus(httpStatus);
            logEntry.setStatus(httpStatus >= 200 && httpStatus < 300 ? "SUCCESS" : "FAILED");
        } catch (Exception ex) {
            log.warn("Webhook 手动重试失败: id={} partnerId={}", deliveryLogId, source.getPartnerId(), ex);
            logEntry.setHttpStatus(-1);
            logEntry.setStatus("FAILED");
        }

        deliveryLogMapper.insert(logEntry);
        WebhookDeliveryLogDO result = new WebhookDeliveryLogDO();
        result.setId(logEntry.getId());
        result.setPartnerId(logEntry.getPartnerId());
        result.setEventType(logEntry.getEventType());
        result.setEventId(logEntry.getEventId());
        result.setResourceType(logEntry.getResourceType());
        result.setResourceId(logEntry.getResourceId());
        result.setResourceIdsJson(logEntry.getResourceIdsJson());
        result.setTriggerSource(logEntry.getTriggerSource());
        result.setPayloadJson(logEntry.getPayloadJson());
        result.setCallbackUrl(logEntry.getCallbackUrl());
        result.setHttpStatus(logEntry.getHttpStatus());
        result.setRetryCount(logEntry.getRetryCount());
        result.setStatus(logEntry.getStatus());
        result.setCreatedAt(logEntry.getCreatedAt());
        return result;
    }

    @Override
    public List<WebhookDeliveryLogDO> listRelatedAttempts(WebhookDeliveryLogDO source) {
        if (source == null || !StringUtils.hasText(source.getPartnerId())) {
            return Collections.emptyList();
        }
        String eventId = StringUtils.hasText(source.getEventId())
                ? source.getEventId()
                : WebhookDeliverySupport.parseEventId(source.getPayloadJson());
        if (StringUtils.hasText(eventId)) {
            List<WebhookDeliveryLogDO> byEventId =
                    apiInvocationRepository.listByEventId(source.getPartnerId(), eventId);
            if (!byEventId.isEmpty()) {
                return byEventId;
            }
        }
        if (source.getId() != null) {
            WebhookDeliveryLogDO self = apiInvocationRepository.findWebhookDeliveryById(source.getId());
            return self == null ? Collections.emptyList() : Collections.singletonList(self);
        }
        return Collections.emptyList();
    }

    private WebhookDeliveryLogPO createLogEntry(WebhookEvent event, String callbackUrl,
                                                 String payloadJson, int attempt) {
        WebhookDeliveryLogPO po = new WebhookDeliveryLogPO();
        po.setPartnerId(event.getPartnerId());
        po.setEventType(event.getEventType());
        po.setPayloadJson(payloadJson);
        po.setCallbackUrl(callbackUrl);
        po.setRetryCount(attempt);
        po.setTriggerSource(attempt <= 0
                ? WebhookDeliverySupport.TRIGGER_FIRST_ATTEMPT
                : WebhookDeliverySupport.TRIGGER_AUTO_RETRY);
        po.setStatus("PENDING");
        po.setCreatedAt(new Date());
        applyEnvelopeMetadata(po, payloadJson);
        return po;
    }

    private void applyEnvelopeMetadata(WebhookDeliveryLogPO po, String payloadJson) {
        po.setEventId(WebhookDeliverySupport.parseEventId(payloadJson));
        ResourceBinding binding = WebhookDeliverySupport.extractResource(po.getEventType(), payloadJson);
        if (binding != null) {
            po.setResourceType(binding.getResourceType());
            po.setResourceId(binding.getResourceId());
            po.setResourceIdsJson(binding.getResourceIdsJson());
        }
    }
}