package com.vtc.openapi.domain.artifact.service.business;

import com.vtc.openapi.domain.artifact.model.result.ArtifactDownloadResult;
import com.vtc.openapi.domain.artifact.model.result.ArtifactListResult;
import com.vtc.openapi.domain.artifact.model.result.ArtifactMetadataResult;
import com.vtc.openapi.domain.open.model.InvocationContext;
import com.vtc.openapi.ui.dto.admin.WebhookDeliveryLogDTO;

public interface IOpenArtifactDomainService {

    ArtifactMetadataResult get(InvocationContext ctx, String partnerId, String artifactId);

    ArtifactDownloadResult download(InvocationContext ctx, String partnerId, String artifactId);

    ArtifactListResult listByTask(InvocationContext ctx, String partnerId, String taskId,
                                  String exportStage, String artifactSource, int page, int size);

    ArtifactListResult listByExport(InvocationContext ctx, String partnerId, String exportId,
                                    int page, int size);

    /**
     * 工作台 Webhook 行：设置 artifactDownloadable（产物 READY 且未过期）。
     */
    void enrichWebhookDelivery(WebhookDeliveryLogDTO dto);
}
