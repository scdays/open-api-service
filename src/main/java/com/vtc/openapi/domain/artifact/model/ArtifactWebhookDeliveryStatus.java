package com.vtc.openapi.domain.artifact.model;

/**
 * 产物 ARTIFACT_READY Webhook 投递状态。
 */
public final class ArtifactWebhookDeliveryStatus {

    /** 已归档，等待同阶段 EXPORT_READY 后再投递 */
    public static final String PENDING = "PENDING";
    /** 已向 Partner 投递 ARTIFACT_READY */
    public static final String SENT = "SENT";

    private ArtifactWebhookDeliveryStatus() {
    }
}
