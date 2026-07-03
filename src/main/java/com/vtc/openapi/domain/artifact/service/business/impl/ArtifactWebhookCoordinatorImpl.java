package com.vtc.openapi.domain.artifact.service.business.impl;

import com.vtc.openapi.domain.artifact.model.ArtifactWebhookDeliveryStatus;
import com.vtc.openapi.domain.artifact.model.entity.OpenArtifactDO;
import com.vtc.openapi.domain.artifact.repository.IOpenArtifactRepository;
import com.vtc.openapi.domain.artifact.service.business.IArtifactWebhookCoordinator;
import com.vtc.openapi.domain.export.model.ExportStage;
import com.vtc.openapi.domain.export.model.ExportStatus;
import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.export.repository.IOpenExportRepository;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.webhook.model.ArtifactReadyEvent;
import com.vtc.openapi.domain.webhook.service.business.IWebhookPublishService;
import com.vtc.openapi.infra.config.OpenApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Service
public class ArtifactWebhookCoordinatorImpl implements IArtifactWebhookCoordinator {

    private static final Logger log = LoggerFactory.getLogger(ArtifactWebhookCoordinatorImpl.class);
    private static final String PRIMARY_EXPORT_FORMAT = "json";

    private final IOpenArtifactRepository artifactRepository;
    private final IOpenExportRepository exportRepository;
    private final IWebhookPublishService webhookPublishService;
    private final OpenApiProperties properties;

    public ArtifactWebhookCoordinatorImpl(IOpenArtifactRepository artifactRepository,
                                          IOpenExportRepository exportRepository,
                                          IWebhookPublishService webhookPublishService,
                                          OpenApiProperties properties) {
        this.artifactRepository = artifactRepository;
        this.exportRepository = exportRepository;
        this.webhookPublishService = webhookPublishService;
        this.properties = properties;
    }

    @Override
    public void onArtifactArchived(OpenTaskSubDO sub, OpenArtifactDO artifact) {
        if (sub == null || artifact == null) {
            return;
        }
        String exportStage = artifact.getExportStage();
        if (!StringUtils.hasText(exportStage)) {
            return;
        }
        bindExportReference(sub, artifact);
        OpenExportDO readyExport = findReadyPrimaryExport(
                artifact.getPartnerId(), artifact.getTaskId(), exportStage);
        if (readyExport != null) {
            deliverArtifactReady(sub, artifact, readyExport.getExportId());
            return;
        }
        markWebhookPending(artifact);
        log.info("ARTIFACT_READY deferred pending export: artifactId={} taskId={} stage={}",
                artifact.getArtifactId(), artifact.getTaskId(), exportStage);
    }

    @Override
    public void flushPendingAfterExportReady(OpenExportDO export) {
        if (export == null || !ExportStatus.READY.equals(export.getStatus())) {
            return;
        }
        if (!PRIMARY_EXPORT_FORMAT.equalsIgnoreCase(export.getFormat())) {
            return;
        }
        List<OpenArtifactDO> pending = artifactRepository.listPendingWebhookDelivery(
                export.getPartnerId(),
                export.getTaskId(),
                export.getExportStage(),
                export.getVerifyFixJobId(),
                200);
        if (CollectionUtils.isEmpty(pending)) {
            return;
        }
        for (OpenArtifactDO artifact : pending) {
            try {
                deliverArtifactReady(null, artifact, export.getExportId());
            } catch (Exception ex) {
                log.warn("ARTIFACT_READY flush failed artifactId={}: {}",
                        artifact.getArtifactId(), ex.getMessage());
            }
        }
        log.info("ARTIFACT_READY flushed count={} exportId={} stage={}",
                pending.size(), export.getExportId(), export.getExportStage());
    }

    @Override
    public void retryPendingDeliveries(int limit) {
        List<OpenArtifactDO> pending = artifactRepository.listAllPendingWebhookDelivery(Math.max(limit, 1));
        if (CollectionUtils.isEmpty(pending)) {
            return;
        }
        for (OpenArtifactDO artifact : pending) {
            if (artifact == null || !StringUtils.hasText(artifact.getTaskId())) {
                continue;
            }
            OpenExportDO readyExport = findReadyPrimaryExport(
                    artifact.getPartnerId(), artifact.getTaskId(), artifact.getExportStage());
            if (readyExport == null) {
                continue;
            }
            try {
                deliverArtifactReady(null, artifact, readyExport.getExportId());
            } catch (Exception ex) {
                log.warn("ARTIFACT_READY retry failed artifactId={}: {}",
                        artifact.getArtifactId(), ex.getMessage());
            }
        }
    }

    private void bindExportReference(OpenTaskSubDO sub, OpenArtifactDO artifact) {
        String exportId = resolveLinkedExportId(
                artifact.getPartnerId(), artifact.getTaskId(), artifact.getExportStage());
        if (StringUtils.hasText(exportId)) {
            artifact.setExportId(exportId);
        }
        if (sub != null && StringUtils.hasText(sub.getVerifyFixJobId())) {
            artifact.setVerifyFixJobId(sub.getVerifyFixJobId());
        }
        artifact.setUpdatedAt(new Date());
        artifactRepository.updateArtifact(artifact);
    }

    private void markWebhookPending(OpenArtifactDO artifact) {
        artifact.setWebhookDeliveryStatus(ArtifactWebhookDeliveryStatus.PENDING);
        artifact.setUpdatedAt(new Date());
        artifactRepository.updateArtifact(artifact);
    }

    private void deliverArtifactReady(OpenTaskSubDO sub, OpenArtifactDO artifact, String exportId) {
        if (artifact == null || !StringUtils.hasText(artifact.getArtifactId())) {
            return;
        }
        if (!StringUtils.hasText(exportId)) {
            markWebhookPending(artifact);
            return;
        }
        artifact.setExportId(exportId);
        artifact.setWebhookDeliveryStatus(ArtifactWebhookDeliveryStatus.SENT);
        artifact.setUpdatedAt(new Date());
        artifactRepository.updateArtifact(artifact);

        if (!properties.getWebhook().isEnabled()) {
            return;
        }
        ArtifactReadyEvent event = new ArtifactReadyEvent();
        event.setPartnerId(artifact.getPartnerId());
        event.setArtifactId(artifact.getArtifactId());
        event.setTaskId(artifact.getTaskId());
        event.setExtTaskId(artifact.getExtTaskId());
        event.setExportId(exportId);
        event.setExportStage(artifact.getExportStage());
        event.setArtifactSource(artifact.getArtifactSource());
        event.setReportTypeCode(artifact.getReportTypeCode());
        event.setFileName(artifact.getFileName());
        event.setFileFormat(artifact.getFileFormat());
        event.setContentType(artifact.getContentType());
        event.setByteSize(artifact.getByteSize());
        event.setDownloadUrl(artifact.getDownloadUrl());
        String verifyFixJobId = artifact.getVerifyFixJobId();
        if (!StringUtils.hasText(verifyFixJobId) && sub != null) {
            verifyFixJobId = sub.getVerifyFixJobId();
        }
        if (StringUtils.hasText(verifyFixJobId)) {
            event.setVerifyFixJobId(verifyFixJobId);
        }
        webhookPublishService.publishArtifactReady(event);
    }

    private OpenExportDO findReadyPrimaryExport(String partnerId, String taskId, String exportStage) {
        OpenExportDO json = exportRepository.findByStageAndFormat(partnerId, taskId, exportStage, PRIMARY_EXPORT_FORMAT);
        if (json != null && ExportStatus.READY.equals(json.getStatus())) {
            return json;
        }
        OpenExportDO xml = exportRepository.findByStageAndFormat(partnerId, taskId, exportStage, "xml");
        if (xml != null && ExportStatus.READY.equals(xml.getStatus())) {
            return xml;
        }
        return null;
    }

    private String resolveLinkedExportId(String partnerId, String taskId, String exportStage) {
        if (ExportStage.RAW_SCAN_ARCHIVE.equals(exportStage)) {
            return null;
        }
        OpenExportDO json = exportRepository.findByStageAndFormat(partnerId, taskId, exportStage, PRIMARY_EXPORT_FORMAT);
        if (json != null && StringUtils.hasText(json.getExportId())) {
            return json.getExportId();
        }
        OpenExportDO xml = exportRepository.findByStageAndFormat(partnerId, taskId, exportStage, "xml");
        if (xml != null && StringUtils.hasText(xml.getExportId())) {
            return xml.getExportId();
        }
        return null;
    }
}
