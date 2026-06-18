package com.vtc.openapi.infra.adapter.taskcenter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.botany.spore.core.result.Result;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobItemDO;
import com.vtc.openapi.domain.instance.repository.IOpenVerifyFixJobRepository;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.instance.service.business.IVerifyFixJobDomainService;
import com.vtc.openapi.domain.operationcase.service.business.IOperationCaseDomainService;
import com.vtc.openapi.infra.converter.InstanceItemConverter;
import com.vtc.openapi.infra.feign.IVulnTaskCenterScanClient;
import com.vtc.openapi.infra.feign.dto.taskcenter.SocOutsideScanRequest;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * task-center 模式：修复核验受理后下发 SOC 复扫计划（open_verify_fix_job 1:1 VTC plan）。
 */
@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterVerifyFixOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterVerifyFixOrchestrator.class);
    private static final String SCANNER_LM = "1";

    private final IOpenVerifyFixJobRepository verifyFixJobRepository;
    private final IOpenVulnInstanceRepository vulnInstanceRepository;
    private final IVulnTaskCenterScanClient scanClient;
    private final IOperationCaseDomainService operationCaseDomainService;

    public TaskCenterVerifyFixOrchestrator(IOpenVerifyFixJobRepository verifyFixJobRepository,
                                           IOpenVulnInstanceRepository vulnInstanceRepository,
                                           IVulnTaskCenterScanClient scanClient,
                                           IOperationCaseDomainService operationCaseDomainService) {
        this.verifyFixJobRepository = verifyFixJobRepository;
        this.vulnInstanceRepository = vulnInstanceRepository;
        this.scanClient = scanClient;
        this.operationCaseDomainService = operationCaseDomainService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void retryDispatchFailed() {
        List<OpenVerifyFixJobDO> jobs = verifyFixJobRepository.listDispatchFailedJobs(30);
        for (OpenVerifyFixJobDO job : jobs) {
            try {
                List<OpenVerifyFixJobItemDO> items = verifyFixJobRepository.listItemsByJobId(job.getJobId());
                dispatchRescan(job, items);
            } catch (Exception ex) {
                log.warn("verify-fix retry dispatch failed jobId={}: {}", job.getJobId(), ex.getMessage());
            }
        }
    }

    /**
     * 运营案件工作台：对指定 job 重试 VTC 复扫下发。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean retryDispatchForJob(String jobId) {
        if (!StringUtils.hasText(jobId)) {
            return false;
        }
        OpenVerifyFixJobDO job = verifyFixJobRepository.findByJobId(jobId.trim());
        if (job == null) {
            return false;
        }
        if (!IVerifyFixJobDomainService.STATUS_DISPATCH_FAILED.equals(job.getStatus())
                && StringUtils.hasText(job.getCenterPlanId())) {
            return false;
        }
        List<OpenVerifyFixJobItemDO> items = verifyFixJobRepository.listItemsByJobId(job.getJobId());
        dispatchRescan(job, items);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public void dispatchRescan(OpenVerifyFixJobDO job, List<OpenVerifyFixJobItemDO> items) {
        if (job == null || CollectionUtils.isEmpty(items)) {
            return;
        }
        if (StringUtils.hasText(job.getCenterPlanId())) {
            return;
        }
        String hosts = collectTargetHosts(job.getPartnerId(), items);
        if (!StringUtils.hasText(hosts)) {
            markDispatchFailed(job, "复扫目标为空");
            return;
        }

        String subId = resolveRescanSubId(job, items);
        Date now = new Date();
        for (OpenVerifyFixJobItemDO item : items) {
            if (!StringUtils.hasText(item.getRescanSubId())) {
                item.setRescanSubId(subId);
                item.setUpdatedAt(now);
                verifyFixJobRepository.updateItem(item);
            }
        }

        SocOutsideScanRequest soc = new SocOutsideScanRequest();
        soc.setTaskId(TaskCenterSocKeys.socTaskId(subId));
        soc.setTaskName("verify-fix-rescan_" + job.getJobId());
        soc.setInputIp(hosts);
        soc.setTaskType("vuln");
        soc.setScannerType(SCANNER_LM);

        try {
            Result<Map<String, Object>> scanResult = scanClient.createSocScan(soc);
            if (scanResult == null || !Boolean.TRUE.equals(scanResult.getSuccess())) {
                String msg = scanResult != null ? scanResult.getMessage() : "verify-fix soc scan failed";
                markDispatchFailed(job, msg);
                return;
            }
            String planId = extractPlanId(scanResult.getData());
            job.setCenterSubId(subId);
            job.setCenterPlanId(planId);
            job.setScannerType(SCANNER_LM);
            job.setInputIps(hosts);
            job.setStatus(IVerifyFixJobDomainService.STATUS_RUNNING);
            job.setProgress(0);
            job.setErrorMessage(null);
            job.setUpdatedAt(now);
            verifyFixJobRepository.updateJob(job);
            operationCaseDomainService.onVerifyFixJobDispatched(job);
            log.info("verify-fix rescan dispatched jobId={} subId={} planId={} hosts={}",
                    job.getJobId(), subId, planId, hosts);
        } catch (FeignException ex) {
            markDispatchFailed(job, "vuln-task-center 调用失败: HTTP " + ex.status() + " " + ex.getMessage());
        } catch (Exception ex) {
            markDispatchFailed(job, "vuln-task-center 调用异常: " + ex.getMessage());
        }
    }

    private static String resolveRescanSubId(OpenVerifyFixJobDO job, List<OpenVerifyFixJobItemDO> items) {
        for (OpenVerifyFixJobItemDO item : items) {
            if (StringUtils.hasText(item.getRescanSubId())) {
                return item.getRescanSubId();
            }
        }
        if (StringUtils.hasText(job.getCenterSubId())) {
            return job.getCenterSubId();
        }
        return "VFS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private void markDispatchFailed(OpenVerifyFixJobDO job, String message) {
        job.setStatus(IVerifyFixJobDomainService.STATUS_DISPATCH_FAILED);
        job.setErrorMessage(TaskCenterTaskOrchestrator.truncateError(message));
        job.setUpdatedAt(new Date());
        verifyFixJobRepository.updateJob(job);
        operationCaseDomainService.onVerifyFixJobTerminal(job);
        log.warn("verify-fix rescan dispatch failed jobId={} reason={}", job.getJobId(), job.getErrorMessage());
    }

    private String collectTargetHosts(String partnerId, List<OpenVerifyFixJobItemDO> items) {
        Set<String> hosts = new LinkedHashSet<>();
        for (OpenVerifyFixJobItemDO item : items) {
            OpenVulnInstanceDO instance = vulnInstanceRepository.findByPartnerAndVulInfoId(
                    partnerId, item.getVulInfoId());
            if (instance == null) {
                continue;
            }
            String addr = extractAddress(instance);
            if (StringUtils.hasText(addr)) {
                hosts.add(addr.trim());
            }
        }
        return hosts.isEmpty() ? null : String.join(",", hosts);
    }

    private static String extractAddress(OpenVulnInstanceDO instance) {
        if (!StringUtils.hasText(instance.getSnapshotJson())) {
            return null;
        }
        JSONObject snap = JSON.parseObject(instance.getSnapshotJson());
        if (snap == null) {
            return null;
        }
        String addr = snap.getString("vulNetAddr");
        if (StringUtils.hasText(addr)) {
            return addr;
        }
        return InstanceItemConverter.fromSnapshot(instance) != null
                ? InstanceItemConverter.fromSnapshot(instance).getVulNetAddr() : null;
    }

    private static String extractPlanId(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        Object id = data.get("id");
        return id != null ? id.toString() : null;
    }
}
