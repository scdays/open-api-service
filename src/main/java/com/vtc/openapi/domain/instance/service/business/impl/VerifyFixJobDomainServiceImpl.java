package com.vtc.openapi.domain.instance.service.business.impl;

import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.export.service.business.IExportAssemblyDomainService;
import com.vtc.openapi.domain.export.service.business.VerifyFixItem;
import com.vtc.openapi.domain.instance.model.command.VerifyFixInstanceCommand;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobItemDO;
import com.vtc.openapi.domain.instance.model.audit.OpenVulnInstanceAudit;
import com.vtc.openapi.domain.instance.model.audit.OpenVulnInstanceAuditContext;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.instance.model.result.InstanceItemResult;
import com.vtc.openapi.domain.instance.model.result.InstanceStateResult;
import com.vtc.openapi.domain.instance.model.support.VerifyFixCompleteMode;
import com.vtc.openapi.domain.instance.repository.IOpenVerifyFixJobRepository;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.instance.repository.IInstanceRepository;
import com.vtc.openapi.domain.instance.service.business.IVerifyFixJobDomainService;
import com.vtc.openapi.domain.instance.service.business.MockVerifyFixRescanMatcher;
import com.vtc.openapi.domain.operationcase.context.OperationCaseContext;
import com.vtc.openapi.domain.operationcase.service.business.IOperationCaseDomainService;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.domain.webhook.service.business.IWebhookPublishService;
import com.vtc.openapi.infra.adapter.mock.MockVerifyFixRescanReportLoader;
import com.vtc.openapi.infra.adapter.taskcenter.TaskCenterVerifyFixPostAcceptDispatcher;
import com.vtc.openapi.infra.converter.InstanceItemConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class VerifyFixJobDomainServiceImpl implements IVerifyFixJobDomainService {

    private static final Logger log = LoggerFactory.getLogger(VerifyFixJobDomainServiceImpl.class);

    private static final int STAT_FIXED = 5;
    private static final int STAT_VERIFIED_FIXED = 6;
    private static final int STAT_VERIFIED_UNFIXED = 7;
    private static final int STAT_VERIFY_FAILED = 10;

    private static final String VERIFY_FIX_STATUS_PENDING = "PENDING";

    private final IOpenVerifyFixJobRepository verifyFixJobRepository;
    private final IOpenVulnInstanceRepository vulnInstanceRepository;
    private final IInstanceRepository instanceRepository;
    private final IOpenTaskRepository openTaskRepository;
    private final IWebhookPublishService webhookPublishService;
    private final MockVerifyFixRescanMatcher rescanMatcher;
    private final MockVerifyFixRescanReportLoader rescanReportLoader;
    private final TaskCenterVerifyFixPostAcceptDispatcher verifyFixPostAcceptDispatcher;
    private final IOperationCaseDomainService operationCaseDomainService;
    private final IExportAssemblyDomainService exportAssemblyDomainService;

    public VerifyFixJobDomainServiceImpl(IOpenVerifyFixJobRepository verifyFixJobRepository,
                                         IOpenVulnInstanceRepository vulnInstanceRepository,
                                         IInstanceRepository instanceRepository,
                                         IOpenTaskRepository openTaskRepository,
                                         IWebhookPublishService webhookPublishService,
                                         MockVerifyFixRescanMatcher rescanMatcher,
                                         @Autowired(required = false) MockVerifyFixRescanReportLoader rescanReportLoader,
                                         @Autowired(required = false) TaskCenterVerifyFixPostAcceptDispatcher verifyFixPostAcceptDispatcher,
                                         IOperationCaseDomainService operationCaseDomainService,
                                         @Autowired(required = false) IExportAssemblyDomainService exportAssemblyDomainService) {
        this.verifyFixJobRepository = verifyFixJobRepository;
        this.vulnInstanceRepository = vulnInstanceRepository;
        this.instanceRepository = instanceRepository;
        this.openTaskRepository = openTaskRepository;
        this.webhookPublishService = webhookPublishService;
        this.rescanMatcher = rescanMatcher;
        this.rescanReportLoader = rescanReportLoader;
        this.verifyFixPostAcceptDispatcher = verifyFixPostAcceptDispatcher;
        this.operationCaseDomainService = operationCaseDomainService;
        this.exportAssemblyDomainService = exportAssemblyDomainService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InstanceStateResult accept(String partnerId, VerifyFixInstanceCommand command, String batchId) {
        List<InstanceStateResult> results = acceptBatch(partnerId, batchId, singleton(command));
        return results.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<InstanceStateResult> acceptBatch(String partnerId, String batchId,
                                                   List<VerifyFixInstanceCommand> commands) {
        if (!StringUtils.hasText(partnerId) || CollectionUtils.isEmpty(commands)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "修复核验请求不能为空");
        }

        String jobId = "VF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Date now = new Date();
        List<OpenVerifyFixJobItemDO> itemRows = new ArrayList<>();
        List<InstanceStateResult> responses = new ArrayList<>();

        for (VerifyFixInstanceCommand command : commands) {
            if (command == null || !StringUtils.hasText(command.getVulInfoId())) {
                throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "vulInfoID 不能为空");
            }
            InstanceItemResult current = requireFixedInstance(partnerId, command.getVulInfoId());
            OpenVulnInstanceDO persisted = vulnInstanceRepository.findByPartnerAndVulInfoId(
                    partnerId, command.getVulInfoId());

            OpenVerifyFixJobItemDO item = new OpenVerifyFixJobItemDO();
            item.setJobId(jobId);
            item.setPartnerId(partnerId);
            item.setVulInfoId(command.getVulInfoId());
            item.setTaskId(persisted != null ? persisted.getTaskId() : null);
            item.setPreviousStat(current.getVulInfoStat());
            item.setItemStatus(ITEM_PENDING);
            item.setCreatedAt(now);
            item.setUpdatedAt(now);
            itemRows.add(item);

            InstanceStateResult response = new InstanceStateResult();
            response.setVulInfoId(command.getVulInfoId());
            response.setVulInfoStat(STAT_FIXED);
            response.setVerifyFixJobId(jobId);
            response.setVerifyFixStatus(VERIFY_FIX_STATUS_PENDING);
            response.setMessage("修复核验已受理，平台将异步复扫");
            response.setTransferTime(resolveTransferTime(command.getTransferTime()));
            responses.add(response);
        }

        OpenVerifyFixJobDO job = new OpenVerifyFixJobDO();
        job.setJobId(jobId);
        job.setPartnerId(partnerId);
        job.setBatchId(batchId);
        job.setStatus(STATUS_PENDING);
        job.setItemCount(itemRows.size());
        job.setRescanImported(false);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        String caseId = OperationCaseContext.getCaseId();
        if (StringUtils.hasText(caseId)) {
            job.setCaseId(caseId);
        }
        verifyFixJobRepository.saveJob(job);
        verifyFixJobRepository.saveItems(itemRows);
        if (StringUtils.hasText(caseId)) {
            operationCaseDomainService.bindVerifyFixJob(caseId, jobId, batchId);
        }

        if (verifyFixPostAcceptDispatcher != null) {
            verifyFixPostAcceptDispatcher.scheduleRescanDispatch(jobId);
        }

        log.info("verify-fix job accepted: jobId={} partnerId={} items={}", jobId, partnerId, itemRows.size());
        return responses;
    }

    @Override
    public OpenVerifyFixJobDO requireJob(String jobId) {
        OpenVerifyFixJobDO job = verifyFixJobRepository.findByJobId(jobId);
        if (job == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "修复核验任务不存在");
        }
        return job;
    }

    @Override
    public List<OpenVerifyFixJobItemDO> listJobItems(String jobId) {
        return verifyFixJobRepository.listItemsByJobId(jobId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeJob(String jobId, VerifyFixCompleteMode mode) {
        OpenVerifyFixJobDO job = requireJob(jobId);
        if (STATUS_FINISHED.equals(job.getStatus())) {
            log.debug("verify-fix job already finished: {}", jobId);
            return;
        }
        if (mode == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "完成模式不能为空");
        }

        List<OpenVerifyFixJobItemDO> items = verifyFixJobRepository.listItemsByJobId(jobId);
        if (CollectionUtils.isEmpty(items)) {
            markJobFailed(job, "无目标实例");
            return;
        }

        job.setStatus(STATUS_RUNNING);
        job.setUpdatedAt(new Date());
        verifyFixJobRepository.updateJob(job);

        Set<String> rescanKeys = Collections.emptySet();
        if (VerifyFixCompleteMode.COMPARE_RESCAN.equals(mode)) {
            rescanKeys = loadRescanFingerprintKeys(jobId, items);
            if (CollectionUtils.isEmpty(rescanKeys)) {
                markJobFailed(job, "未找到复扫报告，请先导入 XML 或确保关联任务已有 source.xml");
                return;
            }
        }

        List<VerifyFixItem> webhookItems = new ArrayList<>();
        boolean anyFailed = false;
        for (OpenVerifyFixJobItemDO item : items) {
            int resultStat = resolveResultStat(item, mode, rescanKeys);
            if (resultStat == STAT_VERIFY_FAILED) {
                anyFailed = true;
            }
            applyItemResult(item, resultStat);
            VerifyFixItem webhookItem = new VerifyFixItem();
            webhookItem.setVulInfoId(item.getVulInfoId());
            webhookItem.setVulInfoStat(resultStat);
            webhookItem.setPreviousVulInfoStat(item.getPreviousStat());
            webhookItems.add(webhookItem);
        }

        Date now = new Date();
        job.setStatus(anyFailed ? STATUS_FAILED : STATUS_FINISHED);
        job.setFinishedAt(now);
        job.setUpdatedAt(now);
        job.setErrorMessage(anyFailed ? "部分实例核验失败" : null);
        verifyFixJobRepository.updateJob(job);

        String webhookStatus = anyFailed ? STATUS_FAILED : STATUS_FINISHED;
        webhookPublishService.publishVerifyFixCompleted(
                job.getPartnerId(), jobId, job.getBatchId(), webhookItems, webhookStatus);
        operationCaseDomainService.onVerifyFixJobTerminal(job);
        triggerVerifyFixExports(job, jobId, webhookItems);
        log.info("verify-fix job completed: jobId={} mode={} items={}", jobId, mode, webhookItems.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importRescanXmlAndComplete(String jobId, byte[] xmlBytes) {
        if (rescanReportLoader == null) {
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "mock rescan loader not available");
        }
        OpenVerifyFixJobDO job = requireJob(jobId);
        try {
            rescanReportLoader.saveJobRescanXml(jobId, xmlBytes);
        } catch (Exception ex) {
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED,
                    "保存复扫 XML 失败: " + ex.getMessage());
        }
        job.setRescanImported(true);
        job.setUpdatedAt(new Date());
        verifyFixJobRepository.updateJob(job);
        completeJob(jobId, VerifyFixCompleteMode.COMPARE_RESCAN);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeFromRescanCompare(String jobId, Set<String> rescanFingerprintKeys) {
        OpenVerifyFixJobDO job = requireJob(jobId);
        if (STATUS_FINISHED.equals(job.getStatus()) || STATUS_FAILED.equals(job.getStatus())) {
            return;
        }
        List<OpenVerifyFixJobItemDO> items = verifyFixJobRepository.listItemsByJobId(jobId);
        if (CollectionUtils.isEmpty(items)) {
            markJobFailed(job, "无目标实例");
            return;
        }
        Set<String> keys = rescanFingerprintKeys != null ? rescanFingerprintKeys : Collections.emptySet();
        job.setStatus(STATUS_RUNNING);
        job.setUpdatedAt(new Date());
        verifyFixJobRepository.updateJob(job);

        for (OpenVerifyFixJobItemDO item : items) {
            if (ITEM_DONE.equals(item.getItemStatus()) || ITEM_FAILED.equals(item.getItemStatus())) {
                continue;
            }
            applyCompareResult(item, keys);
        }
        job.setRescanImported(true);
        job.setProgress(100);
        job.setUpdatedAt(new Date());
        verifyFixJobRepository.updateJob(job);
        tryFinalizeVerifyFixJob(jobId);
        log.info("verify-fix vtc compare job completed: jobId={} items={}", jobId, items.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeFromRescanCompareForSub(String jobId, String rescanSubId, Set<String> rescanFingerprintKeys) {
        if (!StringUtils.hasText(jobId) || !StringUtils.hasText(rescanSubId)) {
            return;
        }
        OpenVerifyFixJobDO job = verifyFixJobRepository.findByJobId(jobId);
        if (job == null) {
            return;
        }
        if (STATUS_FINISHED.equals(job.getStatus()) || STATUS_FAILED.equals(job.getStatus())) {
            return;
        }
        Set<String> keys = rescanFingerprintKeys != null ? rescanFingerprintKeys : Collections.emptySet();
        List<OpenVerifyFixJobItemDO> items = verifyFixJobRepository.listItemsByJobId(jobId);
        boolean any = false;
        for (OpenVerifyFixJobItemDO item : items) {
            if (!rescanSubId.equals(item.getRescanSubId())) {
                continue;
            }
            if (ITEM_DONE.equals(item.getItemStatus()) || ITEM_FAILED.equals(item.getItemStatus())) {
                continue;
            }
            applyCompareResult(item, keys);
            any = true;
        }
        if (!any) {
            return;
        }
        job.setRescanImported(true);
        job.setUpdatedAt(new Date());
        verifyFixJobRepository.updateJob(job);
        tryFinalizeVerifyFixJob(jobId);
        log.info("verify-fix sub compare jobId={} subId={}", jobId, rescanSubId);
    }

    private void applyCompareResult(OpenVerifyFixJobItemDO item, Set<String> rescanKeys) {
        int resultStat = resolveResultStat(item, VerifyFixCompleteMode.COMPARE_RESCAN, rescanKeys);
        applyItemResultWithRescanFlag(item, resultStat, rescanKeys);
    }

    private void tryFinalizeVerifyFixJob(String jobId) {
        OpenVerifyFixJobDO job = verifyFixJobRepository.findByJobId(jobId);
        if (job == null) {
            return;
        }
        List<OpenVerifyFixJobItemDO> items = verifyFixJobRepository.listItemsByJobId(jobId);
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        boolean allTerminal = true;
        boolean anyFailed = false;
        List<VerifyFixItem> webhookItems = new ArrayList<>();
        for (OpenVerifyFixJobItemDO item : items) {
            if (!ITEM_DONE.equals(item.getItemStatus()) && !ITEM_FAILED.equals(item.getItemStatus())) {
                allTerminal = false;
                break;
            }
            if (ITEM_FAILED.equals(item.getItemStatus())
                    || (item.getResultStat() != null && item.getResultStat() == STAT_VERIFY_FAILED)) {
                anyFailed = true;
            }
            if (item.getResultStat() != null) {
                VerifyFixItem webhookItem = new VerifyFixItem();
                webhookItem.setVulInfoId(item.getVulInfoId());
                webhookItem.setVulInfoStat(item.getResultStat());
                webhookItem.setPreviousVulInfoStat(item.getPreviousStat());
                webhookItems.add(webhookItem);
            }
        }
        if (!allTerminal) {
            return;
        }
        Date now = new Date();
        job.setStatus(anyFailed ? STATUS_FAILED : STATUS_FINISHED);
        job.setFinishedAt(now);
        job.setUpdatedAt(now);
        job.setProgress(100);
        job.setErrorMessage(anyFailed ? "部分实例核验失败" : null);
        verifyFixJobRepository.updateJob(job);

        String webhookStatus = anyFailed ? STATUS_FAILED : STATUS_FINISHED;
        webhookPublishService.publishVerifyFixCompleted(
                job.getPartnerId(), jobId, job.getBatchId(), webhookItems, webhookStatus);
        operationCaseDomainService.onVerifyFixJobTerminal(job);
        triggerVerifyFixExports(job, jobId, webhookItems);
    }

    private void triggerVerifyFixExports(OpenVerifyFixJobDO job, String jobId, List<VerifyFixItem> webhookItems) {
        if (exportAssemblyDomainService == null || job == null || !StringUtils.hasText(job.getPartnerId())) {
            return;
        }
        Set<String> taskIds = new LinkedHashSet<>();
        List<OpenVerifyFixJobItemDO> allItems = verifyFixJobRepository.listItemsByJobId(jobId);
        for (OpenVerifyFixJobItemDO item : allItems) {
            if (item != null && StringUtils.hasText(item.getTaskId())) {
                taskIds.add(item.getTaskId());
            }
        }
        for (String taskId : taskIds) {
            try {
                exportAssemblyDomainService.assembleForVerifyFixScan(
                        job.getPartnerId(), taskId, jobId, webhookItems);
            } catch (Exception ex) {
                log.warn("verify-fix export assembly failed jobId={} taskId={}: {}", jobId, taskId, ex.getMessage());
            }
        }
    }

    /**
     * vul-pass 等内部引擎回调：按条目结果完成（仅 Webhook，不外发）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeFromInternalNotify(String jobId, String vulInfoId, Integer resultStat,
                                           String batchId, boolean jobFailed) {
        if (!StringUtils.hasText(jobId)) {
            return;
        }
        OpenVerifyFixJobDO job = verifyFixJobRepository.findByJobId(jobId);
        if (job == null) {
            log.warn("verify-fix notify ignored, job not found: {}", jobId);
            return;
        }

        List<OpenVerifyFixJobItemDO> items = verifyFixJobRepository.listItemsByJobId(jobId);
        if (CollectionUtils.isEmpty(items)) {
            if (StringUtils.hasText(vulInfoId) && resultStat != null) {
                completeOrphanNotify(job, vulInfoId, resultStat, batchId, jobFailed);
            }
            return;
        }

        if (STATUS_FINISHED.equals(job.getStatus()) || STATUS_FAILED.equals(job.getStatus())) {
            log.debug("verify-fix job already terminal: {}", jobId);
            return;
        }

        if (StringUtils.hasText(vulInfoId) && resultStat != null) {
            for (OpenVerifyFixJobItemDO item : items) {
                if (vulInfoId.equals(item.getVulInfoId())) {
                    applyItemResult(item, resultStat);
                    break;
                }
            }
        }

        boolean allDone = true;
        List<VerifyFixItem> webhookItems = new ArrayList<>();
        for (OpenVerifyFixJobItemDO item : items) {
            if (!ITEM_DONE.equals(item.getItemStatus()) && item.getResultStat() == null) {
                allDone = false;
            }
            if (item.getResultStat() != null) {
                VerifyFixItem row = new VerifyFixItem();
                row.setVulInfoId(item.getVulInfoId());
                row.setVulInfoStat(item.getResultStat());
                row.setPreviousVulInfoStat(item.getPreviousStat());
                webhookItems.add(row);
            }
        }

        if (!allDone && !jobFailed) {
            return;
        }

        Date now = new Date();
        job.setStatus(jobFailed ? STATUS_FAILED : STATUS_FINISHED);
        job.setFinishedAt(now);
        job.setUpdatedAt(now);
        if (StringUtils.hasText(batchId)) {
            job.setBatchId(batchId);
        }
        verifyFixJobRepository.updateJob(job);

        String status = jobFailed ? STATUS_FAILED : STATUS_FINISHED;
        webhookPublishService.publishVerifyFixCompleted(
                job.getPartnerId(), jobId, job.getBatchId(), webhookItems, status);
        operationCaseDomainService.onVerifyFixJobTerminal(job);
    }

    @Override
    public List<OpenVerifyFixJobDO> listRecentJobs(String partnerId, String status, int limit) {
        if (StringUtils.hasText(partnerId)) {
            return verifyFixJobRepository.listByPartner(partnerId, status, limit);
        }
        return verifyFixJobRepository.listRecent(status, limit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createInternalFromOfflineTask(String partnerId, String taskId,
                                                List<String> vulInfoIds, String batchId) {
        if (!StringUtils.hasText(partnerId) || !StringUtils.hasText(taskId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "partnerId/taskId 不能为空");
        }
        String normalizedTaskId = taskId.trim();
        OpenTaskDO task = openTaskRepository.findByTaskId(normalizedTaskId);
        if (task == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "任务不存在: " + normalizedTaskId);
        }
        if (!partnerId.equals(task.getPartnerId())) {
            throw new OpenApiException(OpenApiConstants.CODE_CROSS_PARTNER, "任务不属于该接入方");
        }
        List<OpenVulnInstanceDO> rows = vulnInstanceRepository.listByPartnerAndTask(
                partnerId, normalizedTaskId, null);
        if (CollectionUtils.isEmpty(rows)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR,
                    "任务下无实例，请先在半人工导入页完成离线入库");
        }
        Set<String> filter = null;
        if (!CollectionUtils.isEmpty(vulInfoIds)) {
            filter = new HashSet<>(vulInfoIds);
        }
        List<VerifyFixInstanceCommand> commands = new ArrayList<>();
        for (OpenVulnInstanceDO row : rows) {
            if (row.getVulInfoStat() == null || row.getVulInfoStat() != STAT_FIXED) {
                continue;
            }
            if (filter != null && !filter.contains(row.getVulInfoId())) {
                continue;
            }
            VerifyFixInstanceCommand command = new VerifyFixInstanceCommand();
            command.setVulInfoId(row.getVulInfoId());
            commands.add(command);
        }
        if (commands.isEmpty()) {
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID,
                    "无 stat=5（已修复）实例可纳入修复核验，请先完成处置修复");
        }
        String effectiveBatch = StringUtils.hasText(batchId)
                ? batchId.trim()
                : "soc-internal-" + normalizedTaskId;
        List<InstanceStateResult> results = acceptBatch(partnerId, effectiveBatch, commands);
        return results.get(0).getVerifyFixJobId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createJobFromSelection(String partnerId, List<String> vulInfoIds, String batchId) {
        if (!StringUtils.hasText(partnerId) || CollectionUtils.isEmpty(vulInfoIds)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "partnerId/vulInfoIds 不能为空");
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String vulInfoId : vulInfoIds) {
            if (StringUtils.hasText(vulInfoId)) {
                unique.add(vulInfoId.trim());
            }
        }
        if (unique.isEmpty()) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "vulInfoIds 不能为空");
        }
        Map<String, String> vulToPendingJob = new LinkedHashMap<>();
        for (String vulInfoId : unique) {
            OpenVerifyFixJobItemDO pending = verifyFixJobRepository
                    .findLatestPendingItemByPartnerAndVulInfoId(partnerId, vulInfoId);
            if (pending != null && StringUtils.hasText(pending.getJobId())) {
                vulToPendingJob.put(vulInfoId, pending.getJobId());
            }
        }
        Set<String> distinctJobs = new HashSet<>(vulToPendingJob.values());
        if (distinctJobs.size() == 1 && vulToPendingJob.size() == unique.size()) {
            return distinctJobs.iterator().next();
        }
        List<VerifyFixInstanceCommand> commands = new ArrayList<>();
        for (String vulInfoId : unique) {
            VerifyFixInstanceCommand command = new VerifyFixInstanceCommand();
            command.setVulInfoId(vulInfoId);
            commands.add(command);
        }
        String effectiveBatch = StringUtils.hasText(batchId)
                ? batchId.trim()
                : "ops-select-" + System.currentTimeMillis();
        List<InstanceStateResult> results = acceptBatch(partnerId, effectiveBatch, commands);
        return results.get(0).getVerifyFixJobId();
    }

    private void completeOrphanNotify(OpenVerifyFixJobDO job, String vulInfoId, Integer resultStat,
                                      String batchId, boolean jobFailed) {
        OpenVulnInstanceDO instance = vulnInstanceRepository.findByPartnerAndVulInfoId(
                job.getPartnerId(), vulInfoId);
        if (instance != null && resultStat != null && !jobFailed) {
            OpenVulnInstanceAudit audit = OpenVulnInstanceAudit.verifyFixComplete(job.getJobId());
            if (StringUtils.hasText(job.getCaseId())) {
                audit.caseId(job.getCaseId());
            }
            OpenVulnInstanceAuditContext.runWith(audit,
                    () -> vulnInstanceRepository.updateState(
                            instance.getId(), job.getPartnerId(), resultStat, null, null));
        }
        VerifyFixItem item = new VerifyFixItem();
        item.setVulInfoId(vulInfoId);
        item.setVulInfoStat(resultStat);
        item.setPreviousVulInfoStat(STAT_FIXED);
        List<VerifyFixItem> items = new ArrayList<>();
        items.add(item);
        webhookPublishService.publishVerifyFixCompleted(
                job.getPartnerId(), job.getJobId(), batchId, items, jobFailed ? STATUS_FAILED : STATUS_FINISHED);
    }

    private Set<String> loadRescanFingerprintKeys(String jobId, List<OpenVerifyFixJobItemDO> items) {
        if (rescanReportLoader == null) {
            return new HashSet<>();
        }
        String fallbackTaskId = null;
        for (OpenVerifyFixJobItemDO item : items) {
            if (StringUtils.hasText(item.getTaskId())) {
                fallbackTaskId = item.getTaskId();
                break;
            }
        }
        List<JSONObject> rescanRows = rescanReportLoader.loadRescanInstances(jobId, fallbackTaskId);
        if (CollectionUtils.isEmpty(rescanRows) && StringUtils.hasText(fallbackTaskId)) {
            rescanRows = rescanReportLoader.loadFromTaskInstancesJson(fallbackTaskId);
        }
        List<JSONObject> vulRows = new ArrayList<>();
        for (JSONObject row : rescanRows) {
            if (rescanMatcher.isVulnerabilityRow(row)) {
                vulRows.add(row);
            }
        }
        return rescanMatcher.buildFingerprintKeys(vulRows);
    }

    private int resolveResultStat(OpenVerifyFixJobItemDO item, VerifyFixCompleteMode mode,
                                  Set<String> rescanKeys) {
        if (VerifyFixCompleteMode.ALL_FIXED.equals(mode)) {
            return STAT_VERIFIED_FIXED;
        }
        if (VerifyFixCompleteMode.ALL_UNFIXED.equals(mode)) {
            return STAT_VERIFIED_UNFIXED;
        }
        OpenVulnInstanceDO instance = vulnInstanceRepository.findByPartnerAndVulInfoId(
                item.getPartnerId(), item.getVulInfoId());
        if (instance == null) {
            return STAT_VERIFY_FAILED;
        }
        boolean stillPresent = rescanMatcher.isStillPresent(instance, rescanKeys);
        return stillPresent ? STAT_VERIFIED_UNFIXED : STAT_VERIFIED_FIXED;
    }

    private void applyItemResult(OpenVerifyFixJobItemDO item, int resultStat) {
        applyItemResultWithRescanFlag(item, resultStat, null);
    }

    private void applyItemResultWithRescanFlag(OpenVerifyFixJobItemDO item, int resultStat,
                                               Set<String> rescanKeys) {
        OpenVulnInstanceDO instance = vulnInstanceRepository.findByPartnerAndVulInfoId(
                item.getPartnerId(), item.getVulInfoId());
        if (instance != null && resultStat != STAT_VERIFY_FAILED) {
            OpenVulnInstanceAudit audit = OpenVulnInstanceAudit.verifyFixComplete(item.getJobId());
            OpenVerifyFixJobDO job = verifyFixJobRepository.findByJobId(item.getJobId());
            if (job != null && StringUtils.hasText(job.getCaseId())) {
                audit.caseId(job.getCaseId());
            }
            OpenVulnInstanceAuditContext.runWith(audit,
                    () -> vulnInstanceRepository.updateState(
                            instance.getId(), item.getPartnerId(), resultStat, null, null));
        }
        if (rescanKeys != null) {
            boolean matched = instance != null && rescanMatcher.isStillPresent(instance, rescanKeys);
            item.setRescanMatched(matched);
        }
        item.setResultStat(resultStat);
        item.setItemStatus(resultStat == STAT_VERIFY_FAILED ? ITEM_FAILED : ITEM_DONE);
        item.setUpdatedAt(new Date());
        verifyFixJobRepository.updateItem(item);
    }

    private void markJobFailed(OpenVerifyFixJobDO job, String error) {
        job.setStatus(STATUS_FAILED);
        job.setErrorMessage(error);
        job.setUpdatedAt(new Date());
        verifyFixJobRepository.updateJob(job);
        operationCaseDomainService.onVerifyFixJobTerminal(job);
        log.warn("verify-fix job failed: jobId={} reason={}", job.getJobId(), error);
    }

    private InstanceItemResult requireFixedInstance(String partnerId, String vulInfoId) {
        if (!StringUtils.hasText(partnerId) || !StringUtils.hasText(vulInfoId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "partnerId/vulInfoId 不能为空");
        }
        OpenVulnInstanceDO row = vulnInstanceRepository.findByPartnerAndVulInfoId(
                partnerId.trim(), vulInfoId.trim());
        InstanceItemResult current = InstanceItemConverter.fromSnapshot(row);
        if (current == null) {
            current = instanceRepository.findByVulInfoId(partnerId, vulInfoId);
        }
        if (current == null) {
            throw new OpenApiException(OpenApiConstants.CODE_CROSS_PARTNER, "实例不存在或无权访问");
        }
        if (current.getVulInfoStat() == null || current.getVulInfoStat() != STAT_FIXED) {
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID,
                    "实例当前状态不允许核验修复，当前状态: " + current.getVulInfoStat());
        }
        return current;
    }

    private static List<VerifyFixInstanceCommand> singleton(VerifyFixInstanceCommand command) {
        List<VerifyFixInstanceCommand> list = new ArrayList<>(1);
        list.add(command);
        return list;
    }

    private static String resolveTransferTime(String transferTime) {
        if (StringUtils.hasText(transferTime)) {
            return transferTime.trim();
        }
        return String.valueOf(System.currentTimeMillis() / 1000);
    }
}
