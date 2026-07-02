package com.vtc.openapi.domain.export.service.business.impl;

import com.vtc.openapi.domain.export.model.ExportStage;
import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.export.repository.IOpenExportRepository;
import com.vtc.openapi.domain.export.service.business.IExportDownloadPolicy;
import com.vtc.openapi.infra.config.OpenApiProperties;
import com.vtc.openapi.ui.dto.admin.WebhookDeliveryLogDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Set;

@Service
public class ExportDownloadPolicyImpl implements IExportDownloadPolicy {

    private static final String STATUS_READY = "READY";

    private final IOpenExportRepository exportRepository;
    private final OpenApiProperties properties;

    public ExportDownloadPolicyImpl(IOpenExportRepository exportRepository,
                                    OpenApiProperties properties) {
        this.exportRepository = exportRepository;
        this.properties = properties;
    }

    @Override
    public Set<String> resolveDownloadableStages(String partnerId) {
        // partner_webhook_config 已迁移至 platform-admin 控制面，open-api-service 不再读取该表；
        // downloadable_stages 统一取本地配置 OpenApiProperties.export.downloadableStages，
        // 未配置时 effectiveDownloadableStages() 返回默认白名单（TASK_COMPLETED/VERIFY_SCAN/VERIFY_FIX_SCAN）。
        // partnerId 入参保留以兼容接口签名，当前不参与解析。
        return properties.getExport().effectiveDownloadableStages();
    }

    @Override
    public boolean isStageDownloadable(String partnerId, String exportStage) {
        if (!StringUtils.hasText(exportStage)) {
            return false;
        }
        if (ExportStage.RAW_SCAN_ARCHIVE.equals(exportStage)) {
            return false;
        }
        return resolveDownloadableStages(partnerId).contains(exportStage);
    }

    @Override
    public boolean isDownloadable(String partnerId, String exportId) {
        if (!StringUtils.hasText(exportId)) {
            return false;
        }
        OpenExportDO row = exportRepository.findByExportId(exportId.trim());
        return isExportRowDownloadable(partnerId, row);
    }

    @Override
    public void enrichWebhookDelivery(WebhookDeliveryLogDTO dto) {
        if (dto == null) {
            return;
        }
        if (!StringUtils.hasText(dto.getExportId())) {
            dto.setExportDownloadable(Boolean.FALSE);
            return;
        }
        OpenExportDO row = exportRepository.findByExportId(dto.getExportId().trim());
        if (row != null && StringUtils.hasText(row.getExportStage())) {
            dto.setExportStage(row.getExportStage());
        }
        dto.setExportDownloadable(isExportRowDownloadable(dto.getPartnerId(), row));
    }

    private boolean isExportRowDownloadable(String partnerId, OpenExportDO row) {
        if (row == null || !STATUS_READY.equals(row.getStatus())) {
            return false;
        }
        if (row.getExpiresAt() != null && row.getExpiresAt().before(new Date())) {
            return false;
        }
        return isStageDownloadable(partnerId, row.getExportStage());
    }
}
