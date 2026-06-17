package com.vtc.openapi.domain.instance.service.business.impl;

import com.vtc.openapi.domain.export.service.business.IExportAssemblyDomainService;
import com.vtc.openapi.domain.export.service.business.VerifyFixItem;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.instance.service.business.IVerifyFixCompletionDomainService;
import com.vtc.openapi.domain.webhook.service.business.IWebhookPublishService;
import com.vtc.openapi.ui.dto.internal.VerifyFixCompletedNotifyRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
public class VerifyFixCompletionDomainServiceImpl implements IVerifyFixCompletionDomainService {

    private static final Logger log = LoggerFactory.getLogger(VerifyFixCompletionDomainServiceImpl.class);
    private static final int STAT_FIXED = 5;

    private final IOpenVulnInstanceRepository vulnInstanceRepository;
    private final IExportAssemblyDomainService exportAssemblyDomainService;
    private final IWebhookPublishService webhookPublishService;

    public VerifyFixCompletionDomainServiceImpl(IOpenVulnInstanceRepository vulnInstanceRepository,
                                                IExportAssemblyDomainService exportAssemblyDomainService,
                                                IWebhookPublishService webhookPublishService) {
        this.vulnInstanceRepository = vulnInstanceRepository;
        this.exportAssemblyDomainService = exportAssemblyDomainService;
        this.webhookPublishService = webhookPublishService;
    }

    @Override
    public void onVerifyFixCompleted(String verifyFixJobId, VerifyFixCompletedNotifyRequest request) {
        if (!StringUtils.hasText(verifyFixJobId) || request == null) {
            return;
        }
        if (!StringUtils.hasText(request.getPartnerId()) || !StringUtils.hasText(request.getVulInfoId())) {
            log.warn("verify-fix notify ignored, missing partner or vulInfoId, jobId={}", verifyFixJobId);
            return;
        }

        int previousStat = request.getPreviousStat() != null ? request.getPreviousStat() : STAT_FIXED;
        Integer resultStat = request.getResultStat();
        boolean jobFailed = "FAILED".equalsIgnoreCase(request.getStatus());

        OpenVulnInstanceDO instance = vulnInstanceRepository.findByPartnerAndVulInfoId(
                request.getPartnerId(), request.getVulInfoId());
        if (instance != null && resultStat != null && !jobFailed) {
            vulnInstanceRepository.updateState(
                    instance.getId(), request.getPartnerId(), resultStat, null, null);
        }

        VerifyFixItem item = null;
        if (resultStat != null) {
            item = new VerifyFixItem();
            item.setVulInfoId(request.getVulInfoId());
            item.setVulInfoStat(resultStat);
            item.setPreviousVulInfoStat(previousStat);
        }
        List<VerifyFixItem> items = item != null ? Collections.singletonList(item) : Collections.emptyList();
        String verifyFixStatus = jobFailed ? "FAILED" : "FINISHED";

        String taskId = instance != null ? instance.getTaskId() : null;
        if (StringUtils.hasText(taskId)) {
            exportAssemblyDomainService.assembleForVerifyFixScan(
                    request.getPartnerId(), taskId, verifyFixJobId, items);
            return;
        }

        log.info("verify-fix notify without taskId, webhook only: jobId={} vulInfoId={}",
                verifyFixJobId, request.getVulInfoId());
        if (!CollectionUtils.isEmpty(items) || jobFailed) {
            webhookPublishService.publishVerifyFixCompleted(
                    request.getPartnerId(), verifyFixJobId, request.getBatchId(), items, verifyFixStatus);
        }
    }
}
