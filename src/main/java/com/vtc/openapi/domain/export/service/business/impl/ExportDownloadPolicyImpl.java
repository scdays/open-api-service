package com.vtc.openapi.domain.export.service.business.impl;

import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.export.repository.IOpenExportRepository;
import com.vtc.openapi.domain.export.service.business.IExportDownloadPolicy;
import com.vtc.openapi.domain.partner.model.entity.PartnerWebhookConfigDO;
import com.vtc.openapi.domain.partner.repository.IPartnerRepository;
import com.vtc.openapi.infra.config.OpenApiProperties;
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
        return resolveDownloadableStages(partnerId).contains(exportStage);
    }

    @Override
    public boolean isDownloadable(String partnerId, String exportId) {
        if (!StringUtils.hasText(exportId)) {
            return false;
        }
        OpenExportDO row = exportRepository.findByExportId(exportId.trim());
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
