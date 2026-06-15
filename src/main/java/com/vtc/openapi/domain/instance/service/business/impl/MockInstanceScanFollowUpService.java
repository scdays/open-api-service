package com.vtc.openapi.domain.instance.service.business.impl;

import com.vtc.openapi.domain.export.service.business.IExportAssemblyDomainService;
import com.vtc.openapi.domain.export.service.business.VerifyFixItem;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.instance.service.business.IInstanceScanFollowUpService;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.infra.config.OpenApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.UUID;

/**
 * Mock verify / verify-fix scan export follow-up (delayed VERIFY_SCAN and VERIFY_FIX_SCAN).
 */
@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "mock")
public class MockInstanceScanFollowUpService implements IInstanceScanFollowUpService {

    private static final Logger log = LoggerFactory.getLogger(MockInstanceScanFollowUpService.class);

    private final IOpenVulnInstanceRepository vulnInstanceRepository;
    private final IExportAssemblyDomainService exportAssemblyDomainService;
    private final OpenApiProperties properties;

    public MockInstanceScanFollowUpService(IOpenVulnInstanceRepository vulnInstanceRepository,
                                           IExportAssemblyDomainService exportAssemblyDomainService,
                                           OpenApiProperties properties) {
        this.vulnInstanceRepository = vulnInstanceRepository;
        this.exportAssemblyDomainService = exportAssemblyDomainService;
        this.properties = properties;
    }

    @Async
    @Override
    public void scheduleVerifyScan(String partnerId, String vulInfoId) {
        sleep();
        OpenVulnInstanceDO row = vulnInstanceRepository.findByPartnerAndVulInfoId(partnerId, vulInfoId);
        if (row == null || !StringUtils.hasText(row.getTaskId())) {
            return;
        }
        log.info("Mock VERIFY_SCAN export triggered: taskId={} vulInfoId={}", row.getTaskId(), vulInfoId);
        exportAssemblyDomainService.assembleForVerifyScan(partnerId, row.getTaskId());
    }

    @Async
    @Override
    public void scheduleVerifyFixScan(String partnerId, String vulInfoId, int previousStat, int newStat,
                                      String batchId) {
        sleep();
        OpenVulnInstanceDO row = vulnInstanceRepository.findByPartnerAndVulInfoId(partnerId, vulInfoId);
        if (row == null || !StringUtils.hasText(row.getTaskId())) {
            return;
        }
        VerifyFixItem item = new VerifyFixItem();
        item.setVulInfoId(vulInfoId);
        item.setVulInfoStat(newStat);
        item.setPreviousVulInfoStat(previousStat);
        String jobId = "VF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        log.info("Mock VERIFY_FIX_SCAN export triggered: taskId={} vulInfoId={}", row.getTaskId(), vulInfoId);
        exportAssemblyDomainService.assembleForVerifyFixScan(
                partnerId, row.getTaskId(), jobId, Collections.singletonList(item));
    }

    private void sleep() {
        int seconds = properties.getEngine().getMock().getVerifyScanDelaySeconds();
        try {
            Thread.sleep(Math.max(1, seconds) * 1000L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
