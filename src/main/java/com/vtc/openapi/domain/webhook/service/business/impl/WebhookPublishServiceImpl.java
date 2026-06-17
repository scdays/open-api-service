package com.vtc.openapi.domain.webhook.service.business.impl;

import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.export.service.business.VerifyFixItem;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.webhook.model.WebhookEvent;
import com.vtc.openapi.domain.webhook.model.WebhookEventType;
import com.vtc.openapi.domain.webhook.service.business.IWebhookPublishService;
import com.vtc.openapi.infra.config.OpenApiProperties;
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
        publish(WebhookEventType.EXPORT_READY, task.getPartnerId(), payload);
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
