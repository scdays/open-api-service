package com.vtc.openapi.app.service.impl;

import com.vtc.openapi.app.service.IVerifyFixNotifyAppService;
import com.vtc.openapi.domain.instance.service.business.IVerifyFixJobDomainService;
import com.vtc.openapi.ui.dto.internal.VerifyFixCompletedNotifyRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class VerifyFixNotifyAppServiceImpl implements IVerifyFixNotifyAppService {

    private static final Logger log = LoggerFactory.getLogger(VerifyFixNotifyAppServiceImpl.class);

    private final IVerifyFixJobDomainService verifyFixJobDomainService;

    public VerifyFixNotifyAppServiceImpl(IVerifyFixJobDomainService verifyFixJobDomainService) {
        this.verifyFixJobDomainService = verifyFixJobDomainService;
    }

    @Override
    public Map<String, Object> notifyCompleted(String verifyFixJobId, VerifyFixCompletedNotifyRequest request) {
        log.info("verify-fix completed notify: jobId={}, partnerId={}, vulInfoId={}, status={}",
                verifyFixJobId,
                request != null ? request.getPartnerId() : null,
                request != null ? request.getVulInfoId() : null,
                request != null ? request.getStatus() : null);
        boolean jobFailed = request != null && "FAILED".equalsIgnoreCase(request.getStatus());
        verifyFixJobDomainService.completeFromInternalNotify(
                verifyFixJobId,
                request != null ? request.getVulInfoId() : null,
                request != null ? request.getResultStat() : null,
                request != null ? request.getBatchId() : null,
                jobFailed);
        Map<String, Object> body = new HashMap<>();
        body.put("accepted", true);
        body.put("verifyFixJobId", verifyFixJobId);
        if (request != null && StringUtils.hasText(request.getVulInfoId())) {
            body.put("vulInfoId", request.getVulInfoId());
        }
        return body;
    }
}
