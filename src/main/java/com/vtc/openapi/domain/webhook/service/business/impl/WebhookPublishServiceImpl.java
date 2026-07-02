package com.vtc.openapi.domain.webhook.service.business.impl;

import com.alibaba.fastjson.JSON;
import com.vtc.asset.security.platform.eventbus.api.EventBus;
import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.export.repository.IOpenExportRepository;
import com.vtc.openapi.domain.artifact.model.entity.OpenArtifactDO;
import com.vtc.openapi.domain.artifact.repository.IOpenArtifactRepository;
import com.vtc.openapi.domain.export.service.business.VerifyFixItem;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.open.model.support.WebhookDeliverySupport;
import com.vtc.openapi.domain.open.model.support.WebhookDeliverySupport.ResourceBinding;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.webhook.model.ArtifactReadyEvent;
import com.vtc.openapi.domain.webhook.model.WebhookEventType;
import com.vtc.openapi.domain.webhook.model.event.OpenPlatformWebhookEvent;
import com.vtc.openapi.domain.webhook.service.business.IWebhookPublishService;
import com.vtc.openapi.infra.config.OpenApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WebhookPublishServiceImpl implements IWebhookPublishService {

    /** webhook 事件跨服务发布 topic（platform-admin OpenPlatformWebhookEventHandler 订阅） */
    private static final String WEBHOOK_TOPIC = "open-platform-webhook";

    private static final Logger log = LoggerFactory.getLogger(WebhookPublishServiceImpl.class);

    private final EventBus eventBus;
    private final IOpenVulnInstanceRepository vulnInstanceRepository;
    private final IOpenExportRepository exportRepository;
    private final IOpenArtifactRepository artifactRepository;
    private final OpenApiProperties properties;

    public WebhookPublishServiceImpl(EventBus eventBus,
                                     IOpenVulnInstanceRepository vulnInstanceRepository,
                                     IOpenExportRepository exportRepository,
                                     IOpenArtifactRepository artifactRepository,
                                     OpenApiProperties properties) {
        this.eventBus = eventBus;
        this.vulnInstanceRepository = vulnInstanceRepository;
        this.exportRepository = exportRepository;
        this.artifactRepository = artifactRepository;
        this.properties = properties;
    }

    @Override
    public void publishTaskCompleted(OpenTaskDO task, Map<String, Object> summary) {
        if (!properties.getWebhook().isEnabled() || task == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", task.getTaskId());
        payload.put("extTaskId", task.getExtTaskId());
        payload.put("status", "FINISHED");
        if (summary != null) {
            payload.put("summary", summary);
        } else {
            long total = vulnInstanceRepository.countByPartnerAndTaskId(task.getPartnerId(), task.getTaskId());
            Map<String, Object> s = new HashMap<>();
            s.put("totalInstances", (int) total);
            payload.put("summary", s);
        }
        publish(WebhookEventType.TASK_COMPLETED, task.getPartnerId(), payload);
    }

    @Override
    public void publishTaskFailed(OpenTaskDO task) {
        if (!properties.getWebhook().isEnabled() || task == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", task.getTaskId());
        payload.put("extTaskId", task.getExtTaskId());
        payload.put("status", "FAILED");
        publish(WebhookEventType.TASK_FAILED, task.getPartnerId(), payload);
    }

    @Override
    public void publishExportReady(OpenTaskDO task, OpenExportDO export) {
        if (!properties.getWebhook().isEnabled() || task == null || export == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("exportId", export.getExportId());
        payload.put("taskId", task.getTaskId());
        payload.put("extTaskId", task.getExtTaskId());
        payload.put("reportTemplateId", export.getReportTemplateId());
        payload.put("format", export.getFormat());
        payload.put("exportStage", export.getExportStage());
        payload.put("dataType", export.getDataType());
        payload.put("recordCount", export.getRecordCount());
        payload.put("downloadUrl", export.getDownloadUrl());
        if (org.springframework.util.StringUtils.hasText(export.getVerifyFixJobId())) {
            payload.put("verifyFixJobId", export.getVerifyFixJobId());
        }
        String eventId = publish(WebhookEventType.EXPORT_READY, task.getPartnerId(), payload);
        // 将 event_id 回写到 export，建立业务侧 event_id 关联
        export.setWebhookEventId(eventId);
        exportRepository.updateExport(export);
    }

    @Override
    public void publishArtifactReady(ArtifactReadyEvent event) {
        if (!properties.getWebhook().isEnabled() || event == null) {
            log.warn("ARTIFACT_READY skipped: webhook disabled or event null, enabled={} event={}",
                    properties.getWebhook().isEnabled(), event == null);
            return;
        }
        log.info("ARTIFACT_READY publishing partnerId={} taskId={} artifactId={}",
                event.getPartnerId(), event.getTaskId(), event.getArtifactId());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("artifactId", event.getArtifactId());
        payload.put("taskId", event.getTaskId());
        if (org.springframework.util.StringUtils.hasText(event.getExtTaskId())) {
            payload.put("extTaskId", event.getExtTaskId());
        }
        if (org.springframework.util.StringUtils.hasText(event.getExportId())) {
            payload.put("exportId", event.getExportId());
        }
        payload.put("exportStage", event.getExportStage());
        payload.put("artifactSource", event.getArtifactSource());
        if (event.getReportTypeCode() != null) {
            payload.put("reportTypeCode", event.getReportTypeCode());
        }
        payload.put("fileName", event.getFileName());
        payload.put("fileFormat", event.getFileFormat());
        payload.put("contentType", event.getContentType());
        if (event.getByteSize() != null) {
            payload.put("byteSize", event.getByteSize());
        }
        if (org.springframework.util.StringUtils.hasText(event.getDownloadUrl())) {
            payload.put("downloadUrl", event.getDownloadUrl());
        }
        if (org.springframework.util.StringUtils.hasText(event.getVerifyFixJobId())) {
            payload.put("verifyFixJobId", event.getVerifyFixJobId());
        }
        String eventId = publish(WebhookEventType.ARTIFACT_READY, event.getPartnerId(), payload);
        // 将 event_id 回写到 artifact，建立业务侧 event_id 关联
        event.setWebhookEventId(eventId);
        OpenArtifactDO artifact = artifactRepository.findByArtifactId(event.getArtifactId());
        if (artifact != null) {
            artifact.setWebhookEventId(eventId);
            artifactRepository.updateArtifact(artifact);
        }
    }

    @Override
    public void publishVerifyFixCompleted(String partnerId, String verifyFixJobId, String batchId,
                                          List<VerifyFixItem> items) {
        publishVerifyFixCompleted(partnerId, verifyFixJobId, batchId, items, "FINISHED");
    }

    @Override
    public void publishVerifyFixCompleted(String partnerId, String verifyFixJobId, String batchId,
                                          List<VerifyFixItem> items, String verifyFixStatus) {
        if (!properties.getWebhook().isEnabled() || !org.springframework.util.StringUtils.hasText(partnerId)) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verifyFixJobId", verifyFixJobId != null ? verifyFixJobId : "VF-" + System.currentTimeMillis());
        payload.put("verifyFixStatus", org.springframework.util.StringUtils.hasText(verifyFixStatus)
                ? verifyFixStatus : "FINISHED");
        payload.put("totalCount", CollectionUtils.isEmpty(items) ? 0 : items.size());
        if (batchId != null) {
            payload.put("batchId", batchId);
        }
        List<Map<String, Object>> itemPayloads = new ArrayList<>();
        if (items != null) {
            for (VerifyFixItem item : items) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("vulInfoID", item.getVulInfoId());
                row.put("vulInfoStat", item.getVulInfoStat());
                row.put("previousVulInfoStat", item.getPreviousVulInfoStat());
                itemPayloads.add(row);
            }
        }
        payload.put("items", itemPayloads);
        publish(WebhookEventType.INSTANCE_VERIFY_FIX_COMPLETED, partnerId, payload);
    }

    private String publish(String eventType, String partnerId, Map<String, Object> payload) {
        // 业务侧生成 event_id，作为控制面与业务面的桥梁（回写 open_export / open_artifact）
        String eventId = "evt-" + UUID.randomUUID().toString().replace("-", "");

        // 路由元数据：复用 WebhookDeliverySupport.extractResource 计算 resourceType/resourceId，
        // 与 platform-admin 落库路由一致，确保 push-records 按资源过滤有效。
        // extractResource 解析 {eventId,eventType,partnerId,payload:{...}} envelope，读 envelope.payload 字段。
        Map<String, Object> envelopeForRouting = new LinkedHashMap<>();
        envelopeForRouting.put("eventId", eventId);
        envelopeForRouting.put("eventType", eventType);
        envelopeForRouting.put("partnerId", partnerId);
        envelopeForRouting.put("payload", payload);
        ResourceBinding binding = WebhookDeliverySupport.extractResource(eventType, JSON.toJSONString(envelopeForRouting));

        OpenPlatformWebhookEvent event = new OpenPlatformWebhookEvent();
        event.setEventId(eventId);
        event.setEventName(eventType);
        event.setPartnerId(partnerId);
        event.setPayload(payload);
        event.setOccurredAt(System.currentTimeMillis());
        event.setResourceType(binding.getResourceType());
        event.setResourceId(binding.getResourceId());

        // 事务提交后发布；无事务时降级为异步发布。避免「事务回滚却已通知 Partner」。
        eventBus.publishAfterCommit(WEBHOOK_TOPIC, event);

        log.info("Webhook event published to EventBus: topic={}, eventId={}, eventType={}, partnerId={}, resourceType={}, resourceId={}",
                WEBHOOK_TOPIC, eventId, eventType, partnerId, binding.getResourceType(), binding.getResourceId());
        return eventId;
    }
}
