package com.vtc.openapi.infra.adapter.taskcenter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.botany.spore.core.result.Result;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobItemDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.instance.repository.IOpenVerifyFixJobRepository;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.instance.service.business.IVerifyFixJobDomainService;
import com.vtc.openapi.domain.operationcase.service.business.IOperationCaseDomainService;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.domain.task.repository.IOpenTaskSubRepository;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * task-center 模式：修复核验受理后按 scanner_type 创建 open_task_sub(phase=3) 并下发 VTC 复扫。
 */
@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterVerifyFixOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterVerifyFixOrchestrator.class);

    private final IOpenVerifyFixJobRepository verifyFixJobRepository;
    private final IOpenVulnInstanceRepository vulnInstanceRepository;
    private final IOpenTaskRepository openTaskRepository;
    private final IOpenTaskSubRepository openTaskSubRepository;
    private final IVulnTaskCenterScanClient scanClient;
    private final IOperationCaseDomainService operationCaseDomainService;
    private final VerifyFixScannerResolver scannerResolver;

    public TaskCenterVerifyFixOrchestrator(IOpenVerifyFixJobRepository verifyFixJobRepository,
                                           IOpenVulnInstanceRepository vulnInstanceRepository,
                                           IOpenTaskRepository openTaskRepository,
                                           IOpenTaskSubRepository openTaskSubRepository,
                                           IVulnTaskCenterScanClient scanClient,
                                           IOperationCaseDomainService operationCaseDomainService,
                                           VerifyFixScannerResolver scannerResolver) {
        this.verifyFixJobRepository = verifyFixJobRepository;
        this.vulnInstanceRepository = vulnInstanceRepository;
        this.openTaskRepository = openTaskRepository;
        this.openTaskSubRepository = openTaskSubRepository;
        this.scanClient = scanClient;
        this.operationCaseDomainService = operationCaseDomainService;
        this.scannerResolver = scannerResolver;
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

    @Transactional(rollbackFor = Exception.class)
    public boolean retryDispatchForJob(String jobId) {
        if (!StringUtils.hasText(jobId)) {
            return false;
        }
        OpenVerifyFixJobDO job = verifyFixJobRepository.findByJobId(jobId.trim());
        if (job == null) {
            return false;
        }
        if (IVerifyFixJobDomainService.STATUS_FINISHED.equals(job.getStatus())
                || IVerifyFixJobDomainService.STATUS_FAILED.equals(job.getStatus())) {
            return false;
        }
        List<OpenVerifyFixJobItemDO> items = verifyFixJobRepository.listItemsByJobId(job.getJobId());
        List<OpenTaskSubDO> subs = openTaskSubRepository.listByVerifyFixJobId(job.getJobId());
        boolean anyRetryable = subs.stream().anyMatch(this::isSubDispatchRetryable);
        if (!anyRetryable && !subs.isEmpty()) {
            return false;
        }
        if (subs.isEmpty()) {
            dispatchRescan(job, items);
            return true;
        }
        int retried = 0;
        for (OpenTaskSubDO sub : subs) {
            if (!isSubDispatchRetryable(sub)) {
                continue;
            }
            String hosts = collectHostsForSub(job.getPartnerId(), items, sub.getSubId());
            if (tryDispatchSub(job, sub, hosts)) {
                retried++;
            }
        }
        finalizeJobAfterDispatch(job, items);
        return retried > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public void dispatchRescan(OpenVerifyFixJobDO job, List<OpenVerifyFixJobItemDO> items) {
        if (job == null || CollectionUtils.isEmpty(items)) {
            return;
        }
        List<OpenTaskSubDO> existing = openTaskSubRepository.listByVerifyFixJobId(job.getJobId());
        if (!existing.isEmpty() && existing.stream().anyMatch(s -> StringUtils.hasText(s.getCenterPlanId()))) {
            return;
        }

        Date now = new Date();
        Map<String, DispatchGroup> groups = new LinkedHashMap<>();
        int unresolved = 0;

        for (OpenVerifyFixJobItemDO item : items) {
            if (item == null || !StringUtils.hasText(item.getVulInfoId())) {
                continue;
            }
            if (IVerifyFixJobDomainService.ITEM_DONE.equals(item.getItemStatus())) {
                continue;
            }
            VerifyFixScannerResolver.ResolveResult resolved = scannerResolver.resolve(
                    job.getPartnerId(), item.getVulInfoId(), item.getTaskId());
            if (!resolved.resolved) {
                item.setItemStatus(IVerifyFixJobDomainService.ITEM_FAILED);
                item.setResultStat(10);
                item.setUpdatedAt(now);
                verifyFixJobRepository.updateItem(item);
                unresolved++;
                log.warn("verify-fix scanner unresolved jobId={} vulInfoId={}: {}",
                        job.getJobId(), item.getVulInfoId(), resolved.errorMessage);
                continue;
            }
            item.setSourceSubId(resolved.sourceSubId);
            item.setScannerType(resolved.scannerType);
            item.setUpdatedAt(now);
            verifyFixJobRepository.updateItem(item);

            String taskId = resolveAnchorTaskId(item);
            if (!StringUtils.hasText(taskId)) {
                item.setItemStatus(IVerifyFixJobDomainService.ITEM_FAILED);
                item.setResultStat(10);
                item.setUpdatedAt(now);
                verifyFixJobRepository.updateItem(item);
                unresolved++;
                continue;
            }
            String groupKey = taskId + "|" + resolved.scannerType;
            DispatchGroup group = groups.computeIfAbsent(groupKey, k -> new DispatchGroup(taskId, resolved.scannerType));
            group.items.add(item);
            String host = extractHost(job.getPartnerId(), item);
            if (StringUtils.hasText(host)) {
                group.hosts.add(host.trim());
            }
        }

        expandCrossScanGroups(job, groups, items);

        int dispatchSuccess = 0;
        int dispatchFailed = 0;
        StringBuilder errors = new StringBuilder();

        for (DispatchGroup group : groups.values()) {
            if (group.hosts.isEmpty()) {
                for (OpenVerifyFixJobItemDO item : group.items) {
                    item.setItemStatus(IVerifyFixJobDomainService.ITEM_FAILED);
                    item.setResultStat(10);
                    item.setUpdatedAt(now);
                    verifyFixJobRepository.updateItem(item);
                }
                dispatchFailed++;
                errors.append(group.scannerType).append(":复扫目标为空;");
                continue;
            }
            OpenTaskSubDO sub = createVerifyFixSub(job, group, now);
            String hosts = String.join(",", group.hosts);
            for (OpenVerifyFixJobItemDO item : group.items) {
                VerifyFixScannerResolver.ResolveResult resolved = scannerResolver.resolve(
                        job.getPartnerId(), item.getVulInfoId(), item.getTaskId());
                if (resolved.resolved && group.scannerType.equals(resolved.scannerType)) {
                    item.setRescanSubId(sub.getSubId());
                }
                item.setUpdatedAt(now);
                verifyFixJobRepository.updateItem(item);
            }
            if (tryDispatchSub(job, sub, hosts)) {
                dispatchSuccess++;
            } else {
                dispatchFailed++;
                if (StringUtils.hasText(sub.getErrorMessage())) {
                    errors.append(group.scannerType).append(':').append(sub.getErrorMessage()).append(';');
                }
            }
        }

        syncJobSummaryFromSubs(job, groups.values());
        job.setUpdatedAt(now);
        verifyFixJobRepository.updateJob(job);
        finalizeJobAfterDispatch(job, items, dispatchSuccess, dispatchFailed, unresolved, errors.toString());
    }

    private void finalizeJobAfterDispatch(OpenVerifyFixJobDO job, List<OpenVerifyFixJobItemDO> items) {
        List<OpenTaskSubDO> subs = openTaskSubRepository.listByVerifyFixJobId(job.getJobId());
        int success = 0;
        int failed = 0;
        for (OpenTaskSubDO sub : subs) {
            if (StringUtils.hasText(sub.getCenterPlanId())
                    && !TaskCenterSubSupport.STATUS_FAILED.equals(sub.getStatus())) {
                success++;
            } else if (TaskCenterSubSupport.STATUS_FAILED.equals(sub.getStatus())) {
                failed++;
            }
        }
        finalizeJobAfterDispatch(job, items, success, failed, 0, "");
    }

    private void finalizeJobAfterDispatch(OpenVerifyFixJobDO job,
                                          List<OpenVerifyFixJobItemDO> items,
                                          int dispatchSuccess,
                                          int dispatchFailed,
                                          int unresolved,
                                          String errors) {
        Date now = new Date();
        if (dispatchSuccess <= 0) {
            job.setStatus(IVerifyFixJobDomainService.STATUS_DISPATCH_FAILED);
            job.setErrorMessage(TaskCenterTaskOrchestrator.truncateError(
                    StringUtils.hasText(errors) ? errors : "全部扫描器下发失败或未解析到扫描器"));
            job.setUpdatedAt(now);
            verifyFixJobRepository.updateJob(job);
            operationCaseDomainService.onVerifyFixJobTerminal(job);
            return;
        }
        job.setStatus(IVerifyFixJobDomainService.STATUS_RUNNING);
        job.setProgress(0);
        job.setErrorMessage(dispatchFailed > 0 || unresolved > 0
                ? TaskCenterTaskOrchestrator.truncateError(
                "部分实例/扫描器异常: " + errors + (unresolved > 0 ? " unresolved=" + unresolved : ""))
                : null);
        job.setUpdatedAt(now);
        verifyFixJobRepository.updateJob(job);
        operationCaseDomainService.onVerifyFixJobDispatched(job);
        log.info("verify-fix dispatched jobId={} subsOk={} subsFail={} unresolved={}",
                job.getJobId(), dispatchSuccess, dispatchFailed, unresolved);
    }

    private OpenTaskSubDO createVerifyFixSub(OpenVerifyFixJobDO job, DispatchGroup group, Date now) {
        OpenTaskSubDO sub = new OpenTaskSubDO();
        sub.setSubId("SUB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        sub.setTaskId(group.taskId);
        sub.setPartnerId(job.getPartnerId());
        sub.setScanPhase(TaskCenterSubSupport.PHASE_VERIFY_FIX);
        sub.setScannerType(group.scannerType);
        sub.setCenterTaskType("vuln");
        sub.setVerifyFixJobId(job.getJobId());
        sub.setStatus(TaskCenterSubSupport.STATUS_PENDING);
        sub.setProgress(0);
        sub.setInstancesIngested(false);
        sub.setCreatedAt(now);
        sub.setUpdatedAt(now);
        openTaskSubRepository.saveSub(sub);
        return sub;
    }

    private boolean tryDispatchSub(OpenVerifyFixJobDO job, OpenTaskSubDO sub, String hosts) {
        if (sub == null || !StringUtils.hasText(hosts)) {
            return false;
        }
        if (StringUtils.hasText(sub.getCenterPlanId())
                && !TaskCenterSubSupport.STATUS_FAILED.equals(sub.getStatus())) {
            return true;
        }
        SocOutsideScanRequest soc = new SocOutsideScanRequest();
        soc.setTaskId(TaskCenterSocKeys.socTaskId(sub.getSubId()));
        soc.setTaskName("verify-fix-rescan_" + job.getJobId() + "_s" + sub.getScannerType());
        soc.setInputIp(hosts);
        soc.setTaskType("vuln");
        soc.setScannerType(sub.getScannerType());
        try {
            Result<Map<String, Object>> scanResult = scanClient.createSocScan(soc);
            if (scanResult == null || !Boolean.TRUE.equals(scanResult.getSuccess())) {
                String msg = scanResult != null ? scanResult.getMessage() : "verify-fix soc scan failed";
                markSubFailed(sub, msg);
                return false;
            }
            String planId = extractPlanId(scanResult.getData());
            sub.setCenterPlanId(planId);
            sub.setStatus(TaskCenterSubSupport.STATUS_RUNNING);
            sub.setErrorMessage(null);
            sub.setUpdatedAt(new Date());
            openTaskSubRepository.updateSub(sub);
            log.info("verify-fix sub dispatched jobId={} subId={} scanner={} planId={} hosts={}",
                    job.getJobId(), sub.getSubId(), sub.getScannerType(), planId, hosts);
            return true;
        } catch (FeignException ex) {
            markSubFailed(sub, "vuln-task-center 调用失败: HTTP " + ex.status() + " " + ex.getMessage());
            return false;
        } catch (Exception ex) {
            markSubFailed(sub, "vuln-task-center 调用异常: " + ex.getMessage());
            return false;
        }
    }

    private void markSubFailed(OpenTaskSubDO sub, String message) {
        sub.setStatus(TaskCenterSubSupport.STATUS_FAILED);
        sub.setErrorMessage(TaskCenterTaskOrchestrator.truncateError(message));
        sub.setUpdatedAt(new Date());
        openTaskSubRepository.updateSub(sub);
    }

    private boolean isSubDispatchRetryable(OpenTaskSubDO sub) {
        return sub != null
                && (TaskCenterSubSupport.STATUS_FAILED.equals(sub.getStatus())
                || !StringUtils.hasText(sub.getCenterPlanId()));
    }

    private String collectHostsForSub(String partnerId, List<OpenVerifyFixJobItemDO> items, String subId) {
        Set<String> hosts = new LinkedHashSet<>();
        for (OpenVerifyFixJobItemDO item : items) {
            if (item == null || !subId.equals(item.getRescanSubId())) {
                continue;
            }
            String host = extractHost(partnerId, item);
            if (StringUtils.hasText(host)) {
                hosts.add(host.trim());
            }
        }
        return hosts.isEmpty() ? null : String.join(",", hosts);
    }

    private void syncJobSummaryFromSubs(OpenVerifyFixJobDO job, Iterable<DispatchGroup> groups) {
        List<OpenTaskSubDO> subs = openTaskSubRepository.listByVerifyFixJobId(job.getJobId());
        if (subs.isEmpty()) {
            return;
        }
        OpenTaskSubDO first = subs.get(0);
        job.setCenterSubId(first.getSubId());
        job.setCenterPlanId(first.getCenterPlanId());
        job.setScannerType(first.getScannerType());
        Set<String> allHosts = new LinkedHashSet<>();
        for (OpenTaskSubDO sub : subs) {
            String hosts = collectHostsForSub(job.getPartnerId(),
                    verifyFixJobRepository.listItemsByJobId(job.getJobId()), sub.getSubId());
            if (StringUtils.hasText(hosts)) {
                for (String h : hosts.split(",")) {
                    if (StringUtils.hasText(h)) {
                        allHosts.add(h.trim());
                    }
                }
            }
        }
        if (!allHosts.isEmpty()) {
            job.setInputIps(String.join(",", allHosts));
        }
    }

    private String resolveAnchorTaskId(OpenVerifyFixJobItemDO item) {
        if (StringUtils.hasText(item.getTaskId())) {
            OpenTaskDO task = openTaskRepository.findByTaskId(item.getTaskId().trim());
            if (task != null) {
                return task.getTaskId();
            }
        }
        OpenVulnInstanceDO instance = vulnInstanceRepository.findByPartnerAndVulInfoId(
                item.getPartnerId(), item.getVulInfoId());
        if (instance != null && StringUtils.hasText(instance.getTaskId())) {
            return instance.getTaskId().trim();
        }
        return null;
    }

    /**
     * 交叉扫描任务：对原排查阶段全部扫描器各下发一次复扫（与双扫排查对齐）。
     */
    private void expandCrossScanGroups(OpenVerifyFixJobDO job,
                                       Map<String, DispatchGroup> groups,
                                       List<OpenVerifyFixJobItemDO> items) {
        Map<String, List<OpenVerifyFixJobItemDO>> itemsByTask = new LinkedHashMap<>();
        for (OpenVerifyFixJobItemDO item : items) {
            if (item == null || IVerifyFixJobDomainService.ITEM_DONE.equals(item.getItemStatus())) {
                continue;
            }
            String taskId = resolveAnchorTaskId(item);
            if (!StringUtils.hasText(taskId)) {
                continue;
            }
            itemsByTask.computeIfAbsent(taskId, k -> new ArrayList<>()).add(item);
        }
        for (Map.Entry<String, List<OpenVerifyFixJobItemDO>> entry : itemsByTask.entrySet()) {
            String taskId = entry.getKey();
            OpenTaskDO task = openTaskRepository.findByTaskId(taskId);
            if (task == null || !Boolean.TRUE.equals(task.getCrossScan())) {
                continue;
            }
            List<OpenTaskSubDO> surveySubs = openTaskSubRepository.listByTaskIdAndPhase(
                    taskId, TaskCenterSubSupport.PHASE_SURVEY);
            if (CollectionUtils.isEmpty(surveySubs)) {
                continue;
            }
            Set<String> hosts = new LinkedHashSet<>();
            for (OpenVerifyFixJobItemDO item : entry.getValue()) {
                String host = extractHost(job.getPartnerId(), item);
                if (StringUtils.hasText(host)) {
                    hosts.add(host.trim());
                }
            }
            if (hosts.isEmpty()) {
                continue;
            }
            for (OpenTaskSubDO surveySub : surveySubs) {
                if (surveySub == null || !StringUtils.hasText(surveySub.getScannerType())) {
                    continue;
                }
                String scannerType = surveySub.getScannerType().trim();
                String groupKey = taskId + "|" + scannerType;
                DispatchGroup group = groups.computeIfAbsent(groupKey,
                        k -> new DispatchGroup(taskId, scannerType));
                group.hosts.addAll(hosts);
                for (OpenVerifyFixJobItemDO item : entry.getValue()) {
                    if (!group.items.contains(item)) {
                        group.items.add(item);
                    }
                }
            }
        }
    }

    private String extractHost(String partnerId, OpenVerifyFixJobItemDO item) {
        OpenVulnInstanceDO instance = vulnInstanceRepository.findByPartnerAndVulInfoId(
                partnerId, item.getVulInfoId());
        return extractAddress(instance);
    }

    private static String extractAddress(OpenVulnInstanceDO instance) {
        if (instance == null || !StringUtils.hasText(instance.getSnapshotJson())) {
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

    private static final class DispatchGroup {
        final String taskId;
        final String scannerType;
        final List<OpenVerifyFixJobItemDO> items = new ArrayList<>();
        final Set<String> hosts = new LinkedHashSet<>();

        DispatchGroup(String taskId, String scannerType) {
            this.taskId = taskId;
            this.scannerType = scannerType;
        }
    }
}
