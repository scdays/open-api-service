package com.vtc.openapi.domain.open.model.support;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.webhook.model.WebhookEventType;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Webhook ????????eventId / ???????????????????????
 */
public final class WebhookDeliverySupport {

    public static final String TRIGGER_FIRST_ATTEMPT = "FIRST_ATTEMPT";
    public static final String TRIGGER_AUTO_RETRY = "AUTO_RETRY";
    public static final String TRIGGER_MANUAL_RETRY = "MANUAL_RETRY";

    public static final String RESOURCE_TASK = "TASK";
    public static final String RESOURCE_INSTANCE = "INSTANCE";
    public static final String RESOURCE_EXPORT = "EXPORT";

    private WebhookDeliverySupport() {
    }

    public static String parseEventId(String payloadJson) {
        if (!StringUtils.hasText(payloadJson)) {
            return null;
        }
        try {
            JSONObject envelope = JSON.parseObject(payloadJson);
            return envelope == null ? null : envelope.getString("eventId");
        } catch (Exception ex) {
            return null;
        }
    }

    public static ResourceBinding extractResource(String eventType, String payloadJson) {
        ResourceBinding binding = new ResourceBinding();
        if (!StringUtils.hasText(payloadJson)) {
            return binding;
        }
        try {
            JSONObject envelope = JSON.parseObject(payloadJson);
            if (envelope == null) {
                return binding;
            }
            JSONObject payload = envelope.getJSONObject("payload");
            if (payload == null) {
                return binding;
            }
            if (WebhookEventType.TASK_COMPLETED.equals(eventType)
                    || WebhookEventType.TASK_FAILED.equals(eventType)) {
                binding.setResourceType(RESOURCE_TASK);
                binding.setResourceId(firstNonBlank(payload.getString("taskId")));
            } else if (WebhookEventType.EXPORT_READY.equals(eventType)) {
                binding.setResourceType(RESOURCE_EXPORT);
                binding.setResourceId(firstNonBlank(payload.getString("exportId"), payload.getString("taskId")));
                binding.setSecondaryResourceId(payload.getString("taskId"));
            } else if (WebhookEventType.INSTANCE_VERIFY_FIX_COMPLETED.equals(eventType)) {
                binding.setResourceType(RESOURCE_INSTANCE);
                List<String> vulInfoIds = collectVulInfoIds(payload);
                if (!vulInfoIds.isEmpty()) {
                    binding.setResourceId(vulInfoIds.get(0));
                    binding.setResourceIdsJson(JSON.toJSONString(vulInfoIds));
                } else {
                    binding.setResourceId(firstNonBlank(payload.getString("verifyFixJobId")));
                }
            }
        } catch (Exception ignored) {
            // ignore malformed payload
        }
        return binding;
    }

    public static boolean matchesResource(WebhookDeliveryLogResourceView row, String resourceType, String resourceId) {
        if (row == null || !StringUtils.hasText(resourceId)) {
            return false;
        }
        if (StringUtils.hasText(resourceType)
                && StringUtils.hasText(row.getResourceType())
                && !resourceType.equalsIgnoreCase(row.getResourceType())) {
            return false;
        }
        if (resourceId.equals(row.getResourceId())) {
            return true;
        }
        if (StringUtils.hasText(row.getResourceIdsJson())) {
            try {
                List<String> ids = JSON.parseArray(row.getResourceIdsJson(), String.class);
                return ids != null && ids.contains(resourceId);
            } catch (Exception ignored) {
                return row.getResourceIdsJson().contains(resourceId);
            }
        }
        return false;
    }

    /**
     * Webhook ???????????????? resourceId??EXPORT ?? taskId????????????? ID????
     */
    public static String resolveInvocationLinkResourceId(String eventType, String resourceId, String payloadJson) {
        ResourceBinding binding = extractResource(eventType, payloadJson);
        if (WebhookEventType.EXPORT_READY.equals(eventType)) {
            if (binding != null && StringUtils.hasText(binding.getSecondaryResourceId())) {
                return binding.getSecondaryResourceId();
            }
        }
        if (StringUtils.hasText(resourceId)) {
            return resourceId.trim();
        }
        if (binding != null && StringUtils.hasText(binding.getResourceId())) {
            return binding.getResourceId();
        }
        return null;
    }

    public static ExportReadyInfo extractExportReady(String eventType, String payloadJson) {
        if (!WebhookEventType.EXPORT_READY.equals(eventType) || !StringUtils.hasText(payloadJson)) {
            return null;
        }
        try {
            JSONObject envelope = JSON.parseObject(payloadJson);
            if (envelope == null) {
                return null;
            }
            JSONObject payload = envelope.getJSONObject("payload");
            if (payload == null) {
                return null;
            }
            ExportReadyInfo info = new ExportReadyInfo();
            info.setExportId(firstNonBlank(payload.getString("exportId")));
            info.setTaskId(firstNonBlank(payload.getString("taskId")));
            info.setFormat(payload.getString("format"));
            info.setExportStage(payload.getString("exportStage"));
            info.setDownloadUrl(payload.getString("downloadUrl"));
            return StringUtils.hasText(info.getExportId()) ? info : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<String> collectVulInfoIds(JSONObject payload) {
        Set<String> ids = new LinkedHashSet<>();
        JSONArray items = payload.getJSONArray("items");
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                JSONObject item = items.getJSONObject(i);
                if (item != null && StringUtils.hasText(item.getString("vulInfoID"))) {
                    ids.add(item.getString("vulInfoID"));
                }
            }
        }
        return new ArrayList<>(ids);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    public static class ExportReadyInfo {
        private String exportId;
        private String taskId;
        private String format;
        private String exportStage;
        private String downloadUrl;

        public String getExportId() {
            return exportId;
        }

        public void setExportId(String exportId) {
            this.exportId = exportId;
        }

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public String getExportStage() {
            return exportStage;
        }

        public void setExportStage(String exportStage) {
            this.exportStage = exportStage;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public void setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
        }
    }

    public static class ResourceBinding {
        private String resourceType;
        private String resourceId;
        private String secondaryResourceId;
        private String resourceIdsJson;

        public String getResourceType() {
            return resourceType;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }

        public String getResourceId() {
            return resourceId;
        }

        public void setResourceId(String resourceId) {
            this.resourceId = resourceId;
        }

        public String getSecondaryResourceId() {
            return secondaryResourceId;
        }

        public void setSecondaryResourceId(String secondaryResourceId) {
            this.secondaryResourceId = secondaryResourceId;
        }

        public String getResourceIdsJson() {
            return resourceIdsJson;
        }

        public void setResourceIdsJson(String resourceIdsJson) {
            this.resourceIdsJson = resourceIdsJson;
        }
    }

    public interface WebhookDeliveryLogResourceView {
        String getResourceType();

        String getResourceId();

        String getResourceIdsJson();
    }
}
