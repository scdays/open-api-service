package com.vtc.openapi.domain.open.model.support;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.export.model.ExportStage;
import com.vtc.openapi.domain.open.OpenApiOperations;
import com.vtc.openapi.domain.open.model.entity.WebhookDeliveryLogDO;
import com.vtc.openapi.domain.webhook.model.WebhookEventType;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Webhook 投递治理辅助：解析 eventId / 资源绑定、EXPORT 就绪信息等业务字段。
 */
public final class WebhookDeliverySupport {

    public static final String TRIGGER_FIRST_ATTEMPT = "FIRST_ATTEMPT";
    public static final String TRIGGER_AUTO_RETRY = "AUTO_RETRY";
    public static final String TRIGGER_MANUAL_RETRY = "MANUAL_RETRY";

    public static final String RESOURCE_TASK = "TASK";
    public static final String RESOURCE_INSTANCE = "INSTANCE";
    public static final String RESOURCE_EXPORT = "EXPORT";
    public static final String RESOURCE_VERIFY_FIX_JOB = OpenApiOperations.PRIMARY_RESOURCE_VERIFY_FIX_JOB;

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
                if (ExportStage.VERIFY_FIX_SCAN.equals(payload.getString("exportStage"))
                        && StringUtils.hasText(payload.getString("verifyFixJobId"))) {
                    binding.setResourceType(RESOURCE_VERIFY_FIX_JOB);
                    binding.setResourceId(payload.getString("verifyFixJobId"));
                    binding.setSecondaryResourceId(firstNonBlank(payload.getString("exportId")));
                } else {
                    // 外发就绪按 taskId 聚合到任务推送记录（与 TASK_COMPLETED / ARTIFACT_READY 一致）
                    binding.setResourceType(RESOURCE_TASK);
                    binding.setResourceId(firstNonBlank(payload.getString("taskId")));
                    binding.setSecondaryResourceId(firstNonBlank(payload.getString("exportId")));
                }
            } else if (WebhookEventType.INSTANCE_VERIFY_FIX_COMPLETED.equals(eventType)) {
                binding.setResourceType(RESOURCE_VERIFY_FIX_JOB);
                binding.setResourceId(firstNonBlank(payload.getString("verifyFixJobId")));
                List<String> vulInfoIds = collectVulInfoIds(payload);
                if (!vulInfoIds.isEmpty()) {
                    binding.setResourceIdsJson(JSON.toJSONString(vulInfoIds));
                }
            } else if (WebhookEventType.ARTIFACT_READY.equals(eventType)) {
                if (ExportStage.VERIFY_FIX_SCAN.equals(payload.getString("exportStage"))
                        && StringUtils.hasText(payload.getString("verifyFixJobId"))) {
                    binding.setResourceType(RESOURCE_VERIFY_FIX_JOB);
                    binding.setResourceId(payload.getString("verifyFixJobId"));
                    binding.setSecondaryResourceId(payload.getString("artifactId"));
                } else {
                    // 报告产物按 taskId 聚合到任务推送记录（与 TASK_COMPLETED 一致），便于在工作台按任务查看回调
                    binding.setResourceType(RESOURCE_TASK);
                    binding.setResourceId(firstNonBlank(payload.getString("taskId")));
                    binding.setSecondaryResourceId(payload.getString("artifactId"));
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
     * Webhook 关联调用治理时解析 resourceId；EXPORT 事件优先用 taskId，否则取 envelope 中的业务 ID。
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
        return extractExportDeliveryInfo(eventType, payloadJson);
    }

    /**
     * 解析 EXPORT_READY 与 ARTIFACT_READY 的外发产物元数据（exportId/format/exportStage/downloadUrl/taskId）。
     * ARTIFACT_READY 的 format 取 payload.fileFormat。
     */
    public static ExportReadyInfo extractExportDeliveryInfo(String eventType, String payloadJson) {
        if (!WebhookEventType.EXPORT_READY.equals(eventType)
                && !WebhookEventType.ARTIFACT_READY.equals(eventType)) {
            return null;
        }
        if (!StringUtils.hasText(payloadJson)) {
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
            info.setExportStage(payload.getString("exportStage"));
            info.setDownloadUrl(payload.getString("downloadUrl"));
            if (WebhookEventType.ARTIFACT_READY.equals(eventType)) {
                info.setFormat(firstNonBlank(payload.getString("fileFormat"), payload.getString("format")));
            } else {
                info.setFormat(payload.getString("format"));
            }
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

    /**
     * 同一 Webhook 事件（eventId）在多次自动/手动重试下的聚合视图：列表只展示最新一次投递。
     */
    public static final class EventDeliverySummary {
        private final WebhookDeliveryLogDO latest;
        private final int attemptCount;

        public EventDeliverySummary(WebhookDeliveryLogDO latest, int attemptCount) {
            this.latest = latest;
            this.attemptCount = attemptCount;
        }

        public WebhookDeliveryLogDO getLatest() {
            return latest;
        }

        public int getAttemptCount() {
            return attemptCount;
        }
    }

    /**
     * 按 eventId 聚合同一 Webhook 事件的多次投递，保留最新一条并统计总投递次数。
     */
    public static List<EventDeliverySummary> collapseToLatestPerEvent(List<WebhookDeliveryLogDO> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, MutableSummary> grouped = new LinkedHashMap<>();
        for (WebhookDeliveryLogDO row : rows) {
            if (row == null) {
                continue;
            }
            grouped.computeIfAbsent(resolveEventGroupKey(row), key -> new MutableSummary())
                    .observe(row);
        }
        return grouped.values().stream()
                .map(MutableSummary::toSummary)
                .sorted(Comparator.comparing(EventDeliverySummary::getLatest, WebhookDeliverySupport::compareLatestDesc))
                .collect(Collectors.toList());
    }

    private static String resolveEventGroupKey(WebhookDeliveryLogDO row) {
        String partnerId = StringUtils.hasText(row.getPartnerId()) ? row.getPartnerId() : "";
        String eventId = StringUtils.hasText(row.getEventId())
                ? row.getEventId()
                : parseEventId(row.getPayloadJson());
        if (StringUtils.hasText(eventId)) {
            return partnerId + "#evt#" + eventId;
        }
        return partnerId + "#id#" + (row.getId() != null ? row.getId() : System.identityHashCode(row));
    }

    private static int compareLatestDesc(WebhookDeliveryLogDO left, WebhookDeliveryLogDO right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        Date leftAt = left.getCreatedAt();
        Date rightAt = right.getCreatedAt();
        if (leftAt != null && rightAt != null) {
            int compared = rightAt.compareTo(leftAt);
            if (compared != 0) {
                return compared;
            }
        } else if (leftAt != null) {
            return -1;
        } else if (rightAt != null) {
            return 1;
        }
        long leftId = left.getId() != null ? left.getId() : 0L;
        long rightId = right.getId() != null ? right.getId() : 0L;
        return Long.compare(rightId, leftId);
    }

    private static final class MutableSummary {
        private WebhookDeliveryLogDO latest;
        private int attemptCount;

        void observe(WebhookDeliveryLogDO row) {
            attemptCount++;
            if (latest == null || compareLatestDesc(row, latest) < 0) {
                latest = row;
            }
        }

        EventDeliverySummary toSummary() {
            return new EventDeliverySummary(latest, attemptCount);
        }
    }
}
