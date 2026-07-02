package com.vtc.openapi.app.service;

import com.vtc.openapi.app.service.impl.ExportAdminAppServiceImpl;
import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.export.repository.IOpenExportRepository;
import com.vtc.openapi.domain.export.service.business.IOpenExportDomainService;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.open.service.business.IInvocationDomainService;
import com.vtc.openapi.domain.partner.service.business.IPartnerDomainService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExportAdminAppServiceImplTest {

    private IPartnerDomainService partnerDomainService;
    private IOpenExportRepository exportRepository;
    IOpenExportDomainService openExportDomainService;
    private ExportAdminAppServiceImpl service;
    private ExportAdminAppServiceImpl spyService;

    @Before
    public void setUp() {
        exportRepository = mock(IOpenExportRepository.class);
        service = new ExportAdminAppServiceImpl(openExportDomainService,
                exportRepository);
        spyService = Mockito.spy(service);
    }

    @Test
    public void downloadExportByEventId_success_findsExportAndDelegates() {
        String partnerId = "partner-test";
        String eventId = "evt-123";
        String exportId = "export-456";

        OpenExportDO export = new OpenExportDO();
        export.setExportId(exportId);
        export.setPartnerId(partnerId);
        export.setWebhookEventId(eventId);

        when(exportRepository.listByWebhookEventIds(eq(Collections.singleton(eventId))))
                .thenReturn(Collections.singletonList(export));

        Mockito.doReturn(null).when(spyService).downloadExport(eq(partnerId), eq(exportId));

        spyService.downloadExportByEventId(partnerId, eventId);

        verify(exportRepository).listByWebhookEventIds(eq(Collections.singleton(eventId)));
        verify(spyService).downloadExport(eq(partnerId), eq(exportId));
    }

    @Test(expected = OpenApiException.class)
    public void downloadExportByEventId_missingPartnerId_throws() {
        spyService.downloadExportByEventId(null, "evt-123");
    }

    @Test(expected = OpenApiException.class)
    public void downloadExportByEventId_missingEventId_throws() {
        spyService.downloadExportByEventId("partner-test", null);
    }

    @Test(expected = OpenApiException.class)
    public void downloadExportByEventId_noExportFound_throws() {
        String partnerId = "partner-test";
        String eventId = "evt-123";

        when(exportRepository.listByWebhookEventIds(eq(Collections.singleton(eventId))))
                .thenReturn(Collections.emptyList());

        spyService.downloadExportByEventId(partnerId, eventId);
    }

    @Test(expected = OpenApiException.class)
    public void downloadExportByEventId_nullExportList_throws() {
        String partnerId = "partner-test";
        String eventId = "evt-123";

        when(exportRepository.listByWebhookEventIds(eq(Collections.singleton(eventId))))
                .thenReturn(null);

        spyService.downloadExportByEventId(partnerId, eventId);
    }
}
