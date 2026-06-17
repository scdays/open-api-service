package com.vtc.openapi.app.service.impl;

import com.vtc.openapi.app.service.IVerifyFixNotifyAppService;
import com.vtc.openapi.domain.instance.service.business.IVerifyFixCompletionDomainService;
import com.vtc.openapi.ui.dto.internal.VerifyFixCompletedNotifyRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
public class VerifyFixNotifyAppServiceImpl implements IVerifyFixNotifyAppService {

    private static final Logger log = LoggerFactory.getLogger(VerifyFixNotifyAppServiceImpl.class);

    private final IVerifyFixCompletionDomainService completionDomainService;

    public VerifyFixNotifyAppServiceImpl(IVerifyFixCompletionDomainService completionDomainService) {
        this.completionDomainService = completionDomainService;
    }

    @Override
    public Map<String, Object> notifyCompleted(String verifyFixJobId, VerifyFixCompletedNotifyRequest request) {
        log.info("verify-fix completed notify: jobId={}, partnerId={}, vulInfoId={}, status={}",
                verifyFixJobId,
                request != null ? request.getPartnerId() : null,
                request != null ? request.getVulInfoId() : null,
                request != null ? request.getStatus() : null);
        completionDomainService.onVerifyFixCompleted(verifyFixJobId, request);
        return Collections.singletonMap("accepted", true);
    }
}
