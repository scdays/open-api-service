package com.vtc.openapi.infra.adapter.taskcenter;

/**
 * OPEN 子任务与 task-center SOC remarks 关联键（remarks = "SOC" + taskId）。
 */
public final class TaskCenterSocKeys {

    public static final String REMARKS_PREFIX = "SOC";
    public static final String OPEN_SUB_PREFIX = "OPEN-";

    private TaskCenterSocKeys() {
    }

    public static String socTaskId(String openSubId) {
        return OPEN_SUB_PREFIX + openSubId;
    }

    /**
     * 解析 VTC Kafka 回调中的 extTaskId（下发时 remarks / taskId = OPEN-{subId}）。
     */
    public static String parseOpenSubId(String extTaskId) {
        if (extTaskId == null || extTaskId.trim().isEmpty()) {
            return null;
        }
        String trimmed = extTaskId.trim();
        if (trimmed.startsWith(OPEN_SUB_PREFIX)) {
            return trimmed.substring(OPEN_SUB_PREFIX.length());
        }
        if (trimmed.startsWith("SUB-") || trimmed.startsWith("VFS-")) {
            return trimmed;
        }
        return null;
    }
}
