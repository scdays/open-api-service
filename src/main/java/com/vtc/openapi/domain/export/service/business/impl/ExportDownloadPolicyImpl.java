package com.vtc.openapi.domain.export.service.business.impl;

import com.vtc.openapi.domain.export.model.ExportStage;
import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.export.repository.IOpenExportRepository;
import com.vtc.openapi.domain.export.service.business.IExportDownloadPolicy;
import com.vtc.openapi.domain.partner.model.entity.PartnerWebhookConfigDO;
import com.vtc.openapi.domain.partner.repository.IPartnerRepository;
import com.vtc.openapi.infra.config.OpenApiProperties;
import com.vtc.openapi.ui.dto.admin.WebhookDeliveryLogDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExportDownloadPolicyImpl implements IExportDownloadPolicy {

    private static final String STATUS_READY = "READY";

    private final IOpenExportRepository exportRepository;
    private final IPartnerRepository partnerRepository;
    private final OpenApiProperties properties;

    public ExportDownloadPolicyImpl(IOpenExportRepository exportRepository,
                                    IPartnerRepository partnerRepository,
                                    OpenApiProperties properties) {
        this.exportRepository = exportRepository;
        this.partnerRepository = partnerRepository;
        this.properties = properties;
    }

    @Override
    public Set<String> resolveDownloadableStages(String partnerId) {
        if (StringUtils.hasText(partnerId)) {
            PartnerWebhookConfigDO config = partnerRepository.findWebhookConfig(partnerId);
            if (config != null && StringUtils.hasText(config.getDownloadableStages())) {
                return parseStages(config.getDownloadableStages());
            }
        }
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

    private Set<String> parseStages(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
