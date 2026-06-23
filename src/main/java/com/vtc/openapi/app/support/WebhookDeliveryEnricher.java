package com.vtc.openapi.app.support;

import com.vtc.openapi.domain.artifact.service.business.IOpenArtifactDomainService;
import com.vtc.openapi.domain.export.service.business.IExportDownloadPolicy;
import com.vtc.openapi.ui.dto.admin.WebhookDeliveryLogDTO;
import org.springframework.stereotype.Component;

/**
 * 工作台 / 治理面 Webhook 投递行元数据补齐（外发下载 + 产物下载可行性）。
 */
@Component
public class WebhookDeliveryEnricher {

    private final IExportDownloadPolicy exportDownloadPolicy;
    private final IOpenArtifactDomainService openArtifactDomainService;

    public WebhookDeliveryEnricher(IExportDownloadPolicy exportDownloadPolicy,
                                   IOpenArtifactDomainService openArtifactDomainService) {
        this.exportDownloadPolicy = exportDownloadPolicy;
        this.openArtifactDomainService = openArtifactDomainService;
    }

    public void enrich(WebhookDeliveryLogDTO dto) {
        if (dto == null) {
            return;
        }
        exportDownloadPolicy.enrichWebhookDelivery(dto);
        openArtifactDomainService.enrichWebhookDelivery(dto);
    }
}
