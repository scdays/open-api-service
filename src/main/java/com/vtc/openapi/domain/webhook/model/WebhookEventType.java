package com.vtc.openapi.domain.webhook.model;

/**
 * Webhook 事件类型常量（API §6.2）。
 */
public final class WebhookEventType {

    public static final String TASK_COMPLETED = "TASK_COMPLETED";
    public static final String TASK_FAILED = "TASK_FAILED";
    public static final String INSTANCE_VERIFY_FIX_COMPLETED = "INSTANCE_VERIFY_FIX_COMPLETED";
    public static final String EXPORT_READY = "EXPORT_READY";
    public static final String ARTIFACT_READY = "ARTIFACT_READY";

    private WebhookEventType() {
    }
}
