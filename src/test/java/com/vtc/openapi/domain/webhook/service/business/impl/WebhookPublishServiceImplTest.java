package com.vtc.openapi.domain.webhook.service.business.impl;

import com.vtc.asset.security.platform.eventbus.api.EventBus;
import com.vtc.openapi.domain.export.model.ExportStage;
import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.export.repository.IOpenExportRepository;
import com.vtc.openapi.domain.artifact.repository.IOpenArtifactRepository;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.webhook.model.event.OpenPlatformWebhookEvent;
import com.vtc.openapi.infra.config.OpenApiProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 WebhookPublishServiceImpl 经 EventBus 发布 OpenPlatformWebhookEvent，
 * 含 event_id 生成与路由元数据（resourceType/resourceId）解析。
 *
 * @author asset-security
 */
public class WebhookPublishServiceImplTest {

    private EventBus eventBus;
    private IOpenVulnInstanceRepository vulnInstanceRepository;
    private IOpenExportRepository exportRepository;
    private IOpenArtifactRepository artifactRepository;
    private OpenApiProperties properties;

    private WebhookPublishServiceImpl service;

    @Before
    public void setUp() {
        eventBus = mock(EventBus.class);
        vulnInstanceRepository = mock(IOpenVulnInstanceRepository.class);
        exportRepository = mock(IOpenExportRepository.class);
        artifactRepository = mock(IOpenArtifactRepository.class);
        properties = new OpenApiProperties();
        properties.getWebhook().setEnabled(true);
        doNothing().when(eventBus).publishAfterCommit(anyString(), any());

        service = new WebhookPublishServiceImpl(eventBus, vulnInstanceRepository,
                exportRepository, artifactRepository, properties);
    }

    /** EXPORT_READY：按 taskId 聚合，resourceType=TASK、resourceId=taskId。 */
    @Test
    public void publishExportReady_routesByTaskAndPublishesAfterCommit() {
        OpenTaskDO task = new OpenTaskDO();
        task.setPartnerId("SOC-CLIENT");
        task.setTaskId("TASK-7fd32ee50c4c");
        task.setExtTaskId("EXT-1");

        OpenExportDO export = new OpenExportDO();
        export.setExportId("EXP-1");
        export.setFormat("json");
        export.setExportStage(ExportStage.TASK_COMPLETED);
        export.setDownloadUrl("http://x/exp-1");

        service.publishExportReady(task, export);

        ArgumentCaptor<OpenPlatformWebhookEvent> captor =
                ArgumentCaptor.forClass(OpenPlatformWebhookEvent.class);
        verify(eventBus).publishAfterCommit(anyString(), captor.capture());

        OpenPlatformWebhookEvent event = captor.getValue();
        assertEquals("EXPORT_READY", event.getEventName());
        assertEquals("SOC-CLIENT", event.getPartnerId());
        assertEquals("TASK", event.getResourceType());
        assertEquals("TASK-7fd32ee50c4c", event.getResourceId());
        assertNotNull(event.getEventId());
        assertTrue(event.getEventId().startsWith("evt-"));
        assertNotNull(event.getOccurredAt());
        // event_id 回写 open_export
        assertEquals(event.getEventId(), export.getWebhookEventId());
    }

    /** EXPORT_READY + VERIFY_FIX_SCAN 阶段：路由到 VERIFY_FIX_JOB。 */
    @Test
    public void publishExportReady_verifyFixScan_routesByVerifyFixJob() {
        OpenTaskDO task = new OpenTaskDO();
        task.setPartnerId("SOC-CLIENT");
        task.setTaskId("TASK-vf");
        OpenExportDO export = new OpenExportDO();
        export.setExportId("EXP-vf");
        export.setExportStage(ExportStage.VERIFY_FIX_SCAN);
        export.setVerifyFixJobId("VF-94db1ab502aa");

        service.publishExportReady(task, export);

        ArgumentCaptor<OpenPlatformWebhookEvent> captor =
                ArgumentCaptor.forClass(OpenPlatformWebhookEvent.class);
        verify(eventBus).publishAfterCommit(anyString(), captor.capture());
        OpenPlatformWebhookEvent event = captor.getValue();
        assertEquals("VERIFY_FIX_JOB", event.getResourceType());
        assertEquals("VF-94db1ab502aa", event.getResourceId());
    }

    /** webhook 关闭时不发布。 */
    @Test
    public void publishExportReady_disabled_skipsPublish() {
        properties.getWebhook().setEnabled(false);
        OpenTaskDO task = new OpenTaskDO();
        task.setPartnerId("SOC-CLIENT");
        task.setTaskId("TASK-1");
        OpenExportDO export = new OpenExportDO();
        export.setExportId("EXP-1");
        export.setExportStage(ExportStage.TASK_COMPLETED);

        service.publishExportReady(task, export);

        verify(eventBus, org.mockito.Mockito.never())
                .publishAfterCommit(anyString(), any());
    }
}
