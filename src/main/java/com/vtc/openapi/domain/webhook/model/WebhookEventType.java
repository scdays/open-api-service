package com.vtc.openapi.domain.webhook.model;

/**
 * Webhook 事件类型常量。
 */
public final class WebhookEventType {

    /** 任务完成 */
    public static final String TASK_COMPLETED = "TASK_COMPLETED";
    /** 实例状态变更 */
    public static final String INSTANCE_STATUS_CHANGED = "INSTANCE_STATUS_CHANGED";
    /** 外发就绪（P2 暂不实现） */
    public static final String EXPORT_READY = "EXPORT_READY";

    private WebhookEventType() {
    }
}