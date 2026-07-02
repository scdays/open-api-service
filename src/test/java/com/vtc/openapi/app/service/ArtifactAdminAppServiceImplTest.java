package com.vtc.openapi.app.service;

import com.vtc.openapi.app.service.impl.ArtifactAdminAppServiceImpl;
import com.vtc.openapi.domain.artifact.model.entity.OpenArtifactDO;
import com.vtc.openapi.domain.artifact.repository.IOpenArtifactRepository;
import com.vtc.openapi.domain.artifact.service.business.IOpenArtifactDomainService;
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

public class ArtifactAdminAppServiceImplTest {

    private IPartnerDomainService partnerDomainService;
    private IOpenArtifactDomainService openArtifactDomainService;
    private IInvocationDomainService invocationDomainService;
    private IOpenArtifactRepository artifactRepository;

    private ArtifactAdminAppServiceImpl service;
    private ArtifactAdminAppServiceImpl spyService;

    @Before
    public void setUp() {
        partnerDomainService = mock(IPartnerDomainService.class);
        openArtifactDomainService = mock(IOpenArtifactDomainService.class);
        invocationDomainService = mock(IInvocationDomainService.class);
        artifactRepository = mock(IOpenArtifactRepository.class);
        service = new ArtifactAdminAppServiceImpl(
                partnerDomainService,
                openArtifactDomainService,
                invocationDomainService,
                artifactRepository);
        spyService = Mockito.spy(service);
    }

    @Test
    public void downloadArtifactByEventId_success_findsArtifactAndDelegates() {
        String partnerId = "partner-test";
        String eventId = "evt-123";
        String artifactId = "artifact-456";

        OpenArtifactDO artifact = new OpenArtifactDO();
        artifact.setArtifactId(artifactId);
        artifact.setPartnerId(partnerId);
        artifact.setWebhookEventId(eventId);

        when(artifactRepository.listByWebhookEventIds(eq(Collections.singleton(eventId))))
                .thenReturn(Collections.singletonList(artifact));

        Mockito.doReturn(null).when(spyService).downloadArtifact(eq(partnerId), eq(artifactId));

        spyService.downloadArtifactByEventId(partnerId, eventId);

        verify(artifactRepository).listByWebhookEventIds(eq(Collections.singleton(eventId)));
        verify(spyService).downloadArtifact(eq(partnerId), eq(artifactId));
    }

    @Test(expected = OpenApiException.class)
    public void downloadArtifactByEventId_missingPartnerId_throws() {
        spyService.downloadArtifactByEventId(null, "evt-123");
    }

    @Test(expected = OpenApiException.class)
    public void downloadArtifactByEventId_missingEventId_throws() {
        spyService.downloadArtifactByEventId("partner-test", null);
    }

    @Test(expected = OpenApiException.class)
    public void downloadArtifactByEventId_noArtifactFound_throws() {
        String partnerId = "partner-test";
        String eventId = "evt-123";

        when(artifactRepository.listByWebhookEventIds(eq(Collections.singleton(eventId))))
                .thenReturn(Collections.emptyList());

        spyService.downloadArtifactByEventId(partnerId, eventId);
    }

    @Test(expected = OpenApiException.class)
    public void downloadArtifactByEventId_nullArtifactList_throws() {
        String partnerId = "partner-test";
        String eventId = "evt-123";

        when(artifactRepository.listByWebhookEventIds(eq(Collections.singleton(eventId))))
                .thenReturn(null);

        spyService.downloadArtifactByEventId(partnerId, eventId);
    }
}
