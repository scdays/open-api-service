package com.vtc.openapi.app.service;

import com.vtc.openapi.app.service.impl.PartnerTokenAppServiceImpl;
import com.vtc.openapi.common.OpenApiConstants;
import com.vtc.openapi.domain.partner.model.entity.PartnerCredentialDO;
import com.vtc.openapi.domain.partner.model.entity.PartnerDO;
import com.vtc.openapi.infra.config.OpenApiProperties;
import com.vtc.openapi.infra.redis.PartnerTokenRedisStore;
import com.vtc.openapi.infra.repository.PartnerRepository;
import com.vtc.openapi.ui.dto.auth.PartnerTokenIntrospectRequest;
import com.vtc.openapi.ui.dto.auth.PartnerTokenIntrospectResponse;
import com.vtc.openapi.ui.dto.auth.PartnerTokenIssueRequest;
import com.vtc.openapi.ui.dto.auth.PartnerTokenIssueResponse;
import com.vtc.openapi.web.dto.ApiResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PartnerTokenAppServiceImplTest {

    private PartnerRepository partnerRepository;
    private PartnerTokenRedisStore tokenRedisStore;

    private PartnerTokenAppServiceImpl service;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Before
    public void setUp() {
        partnerRepository = mock(PartnerRepository.class);
        tokenRedisStore = mock(PartnerTokenRedisStore.class);
        OpenApiProperties properties = new OpenApiProperties();
        properties.getToken().setExpiresInSeconds(3600L);
        service = new PartnerTokenAppServiceImpl(partnerRepository, tokenRedisStore, properties);
    }

    @Test
    public void issueToken_success_writesRedisContext() {
        String secret = "test-secret-plain";
        PartnerCredentialDO cred = new PartnerCredentialDO();
        cred.setClientId("cli_test");
        cred.setPartnerId("partner-a");
        cred.setClientSecretHash(encoder.encode(secret));
        cred.setStatus(PartnerRepository.STATUS_ACTIVE);

        PartnerDO partner = new PartnerDO();
        partner.setPartnerId("partner-a");
        partner.setStatus(PartnerRepository.STATUS_ACTIVE);

        when(partnerRepository.findCredentialByClientId("cli_test")).thenReturn(cred);
        when(partnerRepository.findByPartnerId("partner-a")).thenReturn(partner);
        when(partnerRepository.listCapabilities("partner-a"))
                .thenReturn(Arrays.asList("TASK_READ", "TASK_WRITE"));

        PartnerTokenIssueRequest req = new PartnerTokenIssueRequest();
        req.setGrantType("client_credentials");
        req.setClientId("cli_test");
        req.setClientSecret(secret);

        ApiResponse<PartnerTokenIssueResponse> resp = service.issueToken(req);
        assertEquals(0, resp.getCode());
        assertNotNull(resp.getData().getAccessToken());
        assertEquals("partner-a", resp.getData().getPartnerId());
        assertEquals(3, resp.getData().getAccessToken().split("\\.").length);

        ArgumentCaptor<PartnerTokenIntrospectResponse> captor =
                ArgumentCaptor.forClass(PartnerTokenIntrospectResponse.class);
        verify(tokenRedisStore).saveToken(eq(resp.getData().getAccessToken()), captor.capture(), eq(3600L));
        assertEquals(OpenApiConstants.SUBJECT_TYPE_PARTNER, captor.getValue().getSubjectType());
        assertEquals("partner-a", captor.getValue().getPartnerId());
        assertEquals(2, captor.getValue().getCapabilities().size());
    }

    @Test
    public void issueToken_disabledPartner_returns40101() {
        PartnerCredentialDO cred = new PartnerCredentialDO();
        cred.setClientId("cli_x");
        cred.setPartnerId("partner-x");
        cred.setClientSecretHash(encoder.encode("s"));
        cred.setStatus(PartnerRepository.STATUS_ACTIVE);
        PartnerDO partner = new PartnerDO();
        partner.setPartnerId("partner-x");
        partner.setStatus(PartnerRepository.STATUS_DISABLED);

        when(partnerRepository.findCredentialByClientId("cli_x")).thenReturn(cred);
        when(partnerRepository.findByPartnerId("partner-x")).thenReturn(partner);

        PartnerTokenIssueRequest req = new PartnerTokenIssueRequest();
        req.setGrantType("client_credentials");
        req.setClientId("cli_x");
        req.setClientSecret("s");

        ApiResponse<PartnerTokenIssueResponse> resp = service.issueToken(req);
        assertEquals(OpenApiConstants.CODE_AUTH_FAILED, resp.getCode());
    }

    @Test
    public void introspect_returnsCachedContext() {
        PartnerTokenIntrospectResponse cached = new PartnerTokenIntrospectResponse();
        cached.setSubjectType("PARTNER");
        cached.setPartnerId("p1");
        cached.setExpiresAt(System.currentTimeMillis() / 1000 + 3600);
        when(tokenRedisStore.getByToken("tok-abc")).thenReturn(cached);

        PartnerTokenIntrospectRequest req = new PartnerTokenIntrospectRequest();
        req.setToken("tok-abc");
        ApiResponse<PartnerTokenIntrospectResponse> resp = service.introspect(req);
        assertEquals(0, resp.getCode());
        assertEquals("p1", resp.getData().getPartnerId());
    }
}
