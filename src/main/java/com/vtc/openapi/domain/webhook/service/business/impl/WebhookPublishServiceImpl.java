package com.vtc.openapi.domain.webhook.service.business.impl;

import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.export.service.business.VerifyFixItem;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.webhook.model.ArtifactReadyEvent;
import com.vtc.openapi.domain.webhook.model.WebhookEvent;
import com.vtc.openapi.domain.webhook.model.WebhookEventType;
import com.vtc.openapi.domain.webhook.service.business.IWebhookPublishService;
import com.vtc.openapi.infra.config.OpenApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WebhookPublishServiceImpl implements IWebhookPublishService {

    private static final Logger log = LoggerFactory.getLogger(WebhookPublishServiceImpl.class);

    private final ApplicationEventPublisher eventPublisher;
    private final IOpenVulnInstanceRepository vulnInstanceRepository;
    private final OpenApiProperties properties;

    public WebhookPublishServiceImpl(ApplicationEventPublisher eventPublisher,
                                     IOpenVulnInstanceRepository vulnInstanceRepository,
                                     OpenApiProperties properties) {
        this.eventPublisher = eventPublisher;
        this.vulnInstanceRepository = vulnInstanceRepository;
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
        publish(WebhookEventType.EXPORT_READY, task.getPartnerId(), payload);
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
        publish(WebhookEventType.ARTIFACT_READY, event.getPartnerId(), payload);
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

    private void publish(String eventType, String partnerId, Map<String, Object> payload) {
        WebhookEvent event = new WebhookEvent();
        event.setEventType(eventType);
        event.setPartnerId(partnerId);
        event.setPayload(payload);
        eventPublisher.publishEvent(event);
    }
}
