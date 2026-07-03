package com.vtc.openapi.domain.artifact.service.business;

import com.vtc.openapi.domain.artifact.model.entity.OpenArtifactDO;
import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;

/**
 * 产物 ARTIFACT_READY Webhook：在外发 EXPORT_READY 之前延迟投递，保证 payload 含 exportId。
 */
public interface IArtifactWebhookCoordinator {

    /**
     * 子任务原始报告归档完成后调用：若同阶段外发已 READY 则立即投递，否则标记待发。
     */
    void onArtifactArchived(OpenTaskSubDO sub, OpenArtifactDO artifact);

    /**
     * 外发文件 READY 且已推送 EXPORT_READY 后，冲刷同阶段待发产物 Webhook。
     */
    void flushPendingAfterExportReady(OpenExportDO export);

    /**
     * 轮询兜底：外发已 READY 但产物 Webhook 仍为 PENDING 的记录。
     */
    void retryPendingDeliveries(int limit);
}
