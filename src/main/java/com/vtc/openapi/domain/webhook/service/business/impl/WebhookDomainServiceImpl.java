package com.vtc.openapi.domain.webhook.service.business.impl;

import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.open.model.entity.WebhookDeliveryLogDO;
import com.vtc.openapi.domain.open.model.support.WebhookDeliverySupport;
import com.vtc.openapi.domain.open.repository.IApiInvocationRepository;
import com.vtc.openapi.domain.partner.model.entity.PartnerWebhookConfigDO;
import com.vtc.openapi.domain.partner.repository.IPartnerRepository;
import com.vtc.openapi.domain.webhook.service.business.IWebhookDomainService;
import com.vtc.openapi.infra.adapter.WebhookDeliveryAdapter;
import com.vtc.openapi.infra.dao.WebhookDeliveryLogMapper;
import com.vtc.openapi.infra.dao.po.WebhookDeliveryLogPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Webhook 投递历史记录领域服务实现。
 * <p>
 * 首次投递已迁移至 eventplus EventBus（WebhookPublishServiceImpl → platform-admin 消费投递）。
 * 本类仅保留历史投递记录的查询 / 手动重试能力（dormant 兼容，前端已改调 platform-admin）。
 */
@Service
public class WebhookDomainServiceImpl implements IWebhookDomainService {

    private static final Logger log = LoggerFactory.getLogger(WebhookDomainServiceImpl.class);

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

    // 注：webhook 投递已迁移至 eventplus EventBus。
    // WebhookPublishServiceImpl 现直接经 EventBus 发布 OpenPlatformWebhookEvent 到 topic open-platform-webhook，
    // 由 platform-admin 的 OpenPlatformWebhookEventHandler 消费并投递（读 platform_admin 库 partner_webhook_config、
    // 写 platform_admin 库 webhook_delivery_log）。本类不再承担首次投递，仅保留历史投递记录的查询/手动重试能力
    // （供 open-api-service 本地 webhook-deliveries 管理端点，前端已改调 platform-admin，此处为 dormant 兼容）。

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
}