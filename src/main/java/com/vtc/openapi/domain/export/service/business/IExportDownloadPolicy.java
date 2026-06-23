package com.vtc.openapi.domain.export.service.business;

import com.vtc.openapi.ui.dto.admin.WebhookDeliveryLogDTO;

import java.util.Set;

/**
 * 外发下载策略：按 Partner 级覆盖或全局默认的 exportStage 白名单判断是否允许下载。
 */
public interface IExportDownloadPolicy {

    /**
     * 解析 Partner 级覆盖或全局默认的允许下载 exportStage 集合。
     * Partner 配置 downloadableStages 非空时用 Partner 的，否则用全局默认。
     */
    Set<String> resolveDownloadableStages(String partnerId);

    /**
     * 判断 exportStage 是否在允许下载白名单内（仅白名单校验，不查 DB status）。
     * 供下载接口在已持有 OpenExportDO 行时调用。
     */
    boolean isStageDownloadable(String partnerId, String exportStage);

    /**
     * 全量判断 exportId 是否可下载：查 open_export → status=READY + 未过期 + stage 在白名单。
     * 供工作台 assembler 设置 DTO.exportDownloadable 时调用。
     */
    boolean isDownloadable(String partnerId, String exportId);

    /**
     * 工作台 Webhook 行：以 open_export.export_stage 为准同步 exportStage，并设置 exportDownloadable。
     */
    void enrichWebhookDelivery(WebhookDeliveryLogDTO dto);
}
