package com.vtc.openapi.domain.artifact.service.business;

import com.vtc.openapi.domain.artifact.model.ArtifactWebhookDeliveryStatus;
import com.vtc.openapi.domain.artifact.model.entity.OpenArtifactDO;
import com.vtc.openapi.domain.artifact.repository.IOpenArtifactRepository;
import com.vtc.openapi.domain.artifact.service.business.impl.ArtifactWebhookCoordinatorImpl;
import com.vtc.openapi.domain.export.model.ExportStage;
import com.vtc.openapi.domain.export.model.ExportStatus;
import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.export.repository.IOpenExportRepository;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.webhook.service.business.IWebhookPublishService;
import com.vtc.openapi.infra.config.OpenApiProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArtifactWebhookCoordinatorImplTest {

    private IOpenArtifactRepository artifactRepository;
    private IOpenExportRepository exportRepository;
    private IWebhookPublishService webhookPublishService;
    private ArtifactWebhookCoordinatorImpl coordinator;

    @Before
    public void setUp() {
        artifactRepository = mock(IOpenArtifactRepository.class);
        exportRepository = mock(IOpenExportRepository.class);
        webhookPublishService = mock(IWebhookPublishService.class);
        OpenApiProperties properties = new OpenApiProperties();
        properties.getWebhook().setEnabled(true);
        coordinator = new ArtifactWebhookCoordinatorImpl(
                artifactRepository, exportRepository, webhookPublishService, properties);
    }

    @Test
    public void defersWebhookWhenExportNotReady() {
        OpenTaskSubDO sub = sub("SUB-1", "TASK-1");
        OpenArtifactDO artifact = artifact("ART-1", "TASK-1");

        when(exportRepository.findByStageAndFormat("P1", "TASK-1", ExportStage.TASK_COMPLETED, "json"))
                .thenReturn(pendingExport("EXP-pending"));
        when(exportRepository.findByStageAndFormat("P1", "TASK-1", ExportStage.TASK_COMPLETED, "xml"))
                .thenReturn(null);

        coordinator.onArtifactArchived(sub, artifact);

        ArgumentCaptor<OpenArtifactDO> captor = ArgumentCaptor.forClass(OpenArtifactDO.class);
        verify(artifactRepository, atLeastOnce()).updateArtifact(captor.capture());
        OpenArtifactDO updated = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(ArtifactWebhookDeliveryStatus.PENDING, updated.getWebhookDeliveryStatus());
        assertEquals("EXP-pending", updated.getExportId());
        verify(webhookPublishService, never()).publishArtifactReady(any());
    }

    @Test
    public void flushesPendingAfterExportReady() {
        OpenExportDO ready = readyExport("EXP-ready", "TASK-1");
        OpenArtifactDO pending = artifact("ART-2", "TASK-1");
        pending.setWebhookDeliveryStatus(ArtifactWebhookDeliveryStatus.PENDING);

        when(artifactRepository.listPendingWebhookDelivery(
                eq("P1"), eq("TASK-1"), eq(ExportStage.TASK_COMPLETED), eq(null), eq(200)))
                .thenReturn(Collections.singletonList(pending));

        coordinator.flushPendingAfterExportReady(ready);

        verify(webhookPublishService).publishArtifactReady(any());
        ArgumentCaptor<OpenArtifactDO> captor = ArgumentCaptor.forClass(OpenArtifactDO.class);
        verify(artifactRepository).updateArtifact(captor.capture());
        assertEquals(ArtifactWebhookDeliveryStatus.SENT, captor.getValue().getWebhookDeliveryStatus());
        assertEquals("EXP-ready", captor.getValue().getExportId());
    }

    private static OpenTaskSubDO sub(String subId, String taskId) {
        OpenTaskSubDO sub = new OpenTaskSubDO();
        sub.setSubId(subId);
        sub.setTaskId(taskId);
        sub.setPartnerId("P1");
        return sub;
    }

    private static OpenArtifactDO artifact(String artifactId, String taskId) {
        OpenArtifactDO artifact = new OpenArtifactDO();
        artifact.setArtifactId(artifactId);
        artifact.setPartnerId("P1");
        artifact.setTaskId(taskId);
        artifact.setExportStage(ExportStage.TASK_COMPLETED);
        artifact.setArtifactSource("SCANNER_RAW");
        artifact.setFileName("report.xml");
        artifact.setFileFormat("xml");
        artifact.setContentType("application/xml");
        artifact.setDownloadUrl("https://example/download");
        return artifact;
    }

    private static OpenExportDO pendingExport(String exportId) {
        OpenExportDO row = new OpenExportDO();
        row.setExportId(exportId);
        row.setStatus(ExportStatus.PENDING);
        return row;
    }

    private static OpenExportDO readyExport(String exportId, String taskId) {
        OpenExportDO row = new OpenExportDO();
        row.setExportId(exportId);
        row.setPartnerId("P1");
        row.setTaskId(taskId);
        row.setExportStage(ExportStage.TASK_COMPLETED);
        row.setFormat("json");
        row.setStatus(ExportStatus.READY);
        return row;
    }
}
