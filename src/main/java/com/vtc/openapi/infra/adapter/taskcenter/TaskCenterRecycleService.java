package com.vtc.openapi.infra.adapter.taskcenter;

import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.instance.model.audit.OpenVulnInstanceAudit;
import com.vtc.openapi.domain.instance.model.audit.OpenVulnInstanceAuditContext;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceLogDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.export.service.business.IExportAssemblyDomainService;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.domain.task.repository.IOpenTaskSubRepository;
import com.vtc.openapi.domain.operationcase.service.business.IOperationCaseDomainService;
import com.vtc.openapi.infra.converter.InstanceItemConverter;
import com.vtc.openapi.infra.feign.dto.taskcenter.TaskCenterSurveyBundle;
import com.vtc.openapi.infra.instance.OpenVulnInstanceLogWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterRecycleService {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterRecycleService.class);

    private final IOpenTaskRepository openTaskRepository;
    private final IOpenTaskSubRepository openTaskSubRepository;
    private final IOpenVulnInstanceRepository vulnInstanceRepository;
    private final TaskCenterScanResultQueryService scanResultQueryService;
    private final TaskCenterSurveyResultsAdapter resultsAdapter;
    private final TaskCenterVerifyMergeService verifyMergeService;
    private final TaskCenterVerifyStatusResolver verifyStatusResolver;
    private final TaskCenterTaskOrchestrator orchestrator;
    private final TaskCenterTaskCompletionCoordinator completionCoordinator;
    private final OpenVulnInstanceLogWriter instanceLogWriter;
    private final IOperationCaseDomainService operationCaseDomainService;
    private final TaskCenterSurveyCaptureRetryPolicy captureRetryPolicy;
    private final IExportAssemblyDomainService exportAssemblyDomainService;

    public TaskCenterRecycleService(IOpenTaskRepository openTaskRepository,
                                    IOpenTaskSubRepository openTaskSubRepository,
                                    IOpenVulnInstanceRepository vulnInstanceRepository,
                                    TaskCenterScanResultQueryService scanResultQueryService,
                                    TaskCenterSurveyResultsAdapter resultsAdapter,
                                    TaskCenterVerifyMergeService verifyMergeService,
                                    TaskCenterVerifyStatusResolver verifyStatusResolver,
                                    TaskCenterTaskOrchestrator orchestrator,
                                    TaskCenterTaskCompletionCoordinator completionCoordinator,
                                    OpenVulnInstanceLogWriter instanceLogWriter,
                                    IOperationCaseDomainService operationCaseDomainService,
                                    TaskCenterSurveyCaptureRetryPolicy captureRetryPolicy,
                                    IExportAssemblyDomainService exportAssemblyDomainService) {
        this.openTaskRepository = openTaskRepository;
        this.openTaskSubRepository = openTaskSubRepository;
        this.vulnInstanceRepository = vulnInstanceRepository;
        this.scanResultQueryService = scanResultQueryService;
        this.resultsAdapter = resultsAdapter;
        this.verifyMergeService = verifyMergeService;
        this.verifyStatusResolver = verifyStatusResolver;
        this.orchestrator = orchestrator;
        this.completionCoordinator = completionCoordinator;
        this.instanceLogWriter = instanceLogWriter;
        this.operationCaseDomainService = operationCaseDomainService;
        this.captureRetryPolicy = captureRetryPolicy;
        this.exportAssemblyDomainService = exportAssemblyDomainService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void tryAdvanceTask(OpenTaskDO task) {
        if (task == null || !StringUtils.hasText(task.getTaskId())) {
            return;
        }
        if ("FAILED".equals(task.getStatus())
                || OpenApiConstants.TASK_DISPATCH_FAILED.equals(task.getStatus())
                || OpenApiConstants.TASK_ACCEPT_ACCEPTED.equals(task.getStatus())) {
            return;
        }
        int phase = task.getTaskPhase() != null ? task.getTaskPhase() : TaskCenterSubSupport.PHASE_SURVEY;
        List<OpenTaskSubDO> subs = openTaskSubRepository.listByTaskIdAndPhase(task.getTaskId(), phase);
        if (CollectionUtils.isEmpty(subs)) {
            return;
        }
        if (!allTerminal(subs)) {
            if (!"FINISHED".equals(task.getStatus())) {
                ingestFinishedSurveySubs(task, subs);
                updateTaskProgress(task, subs);
            }
            return;
        }
        if (anyFailed(subs)) {
            if (!"FAILED".equals(task.getStatus())) {
                markTaskFailed(task, "子扫描任务失败");
            }
            return;
        }
        if (phase == TaskCenterSubSupport.PHASE_SURVEY) {
            handleSurveyPhaseTerminal(task, subs);
        } else if (phase == TaskCenterSubSupport.PHASE_VERIFY) {
            if ("FINISHED".equals(task.getStatus())) {
                return;
            }
            onVerifyPhaseComplete(task, subs);
        }
    }

    /**
     * 单个子任务（单扫描器）排查完成后：落库结果已就绪时立即 ingest 漏洞实例与跃迁日志。
     * 交叉扫描不再等待全部扫描器完成。
     */
    @Transactional(rollbackFor = Exception.class)
    public void ingestSurveySubIfNeeded(OpenTaskDO task, OpenTaskSubDO sub) {
        if (task == null || sub == null) {
            return;
        }
        if (sub.getScanPhase() == null || sub.getScanPhase() != TaskCenterSubSupport.PHASE_SURVEY) {
            return;
        }
        if (!TaskCenterSubSupport.STATUS_FINISHED.equals(sub.getStatus())) {
            return;
        }
        if (Boolean.TRUE.equals(sub.getInstancesIngested())) {
            return;
        }
        if (!"vuln".equals(sub.getCenterTaskType())) {
            markSubInstancesIngested(sub);
            syncTaskInstancesIngestedFlag(task);
            return;
        }
        if (!scanResultQueryService.hasPersistedResults(sub.getSubId())) {
            log.info("survey sub ingest deferred: no persisted scan results taskId={} subId={}",
                    task.getTaskId(), sub.getSubId());
            return;
        }
        TaskCenterSurveyBundle bundle = scanResultQueryService.loadSurveyBundleBySub(sub.getSubId());
        List<JSONObject> rows = resultsAdapter.toVulnInstances(bundle);
        if (CollectionUtils.isEmpty(rows)) {
            markSubInstancesIngested(sub);
            syncTaskInstancesIngestedFlag(task);
            log.info("survey sub ingest skipped empty vulns taskId={} subId={}", task.getTaskId(), sub.getSubId());
            return;
        }
        List<OpenVulnInstanceDO> persistRows = buildPersistRows(task, sub, rows, 1);
        vulnInstanceRepository.batchInsert(persistRows);
        instanceLogWriter.writeIngestBatch(task, persistRows, sub.getSubId(), TaskCenterSubSupport.PHASE_SURVEY);
        markSubInstancesIngested(sub);
        syncTaskInstancesIngestedFlag(task);
        log.info("survey sub ingest finished taskId={} subId={} scanner={} count={}",
                task.getTaskId(), sub.getSubId(), sub.getScannerType(), persistRows.size());
    }

    private void ingestFinishedSurveySubs(OpenTaskDO task, List<OpenTaskSubDO> subs) {
        if (task == null || CollectionUtils.isEmpty(subs)) {
            return;
        }
        int phase = task.getTaskPhase() != null ? task.getTaskPhase() : TaskCenterSubSupport.PHASE_SURVEY;
        if (phase != TaskCenterSubSupport.PHASE_SURVEY) {
            return;
        }
        for (OpenTaskSubDO sub : subs) {
            if (sub != null && TaskCenterSubSupport.STATUS_FINISHED.equals(sub.getStatus())) {
                ingestSurveySubIfNeeded(task, sub);
            }
        }
    }

    /**
     * 排查阶段全部子任务 FINISHED：立即 TASK_COMPLETED；VTC 结果落库就绪后再 EXPORT_READY。
     */
    private void handleSurveyPhaseTerminal(OpenTaskDO task, List<OpenTaskSubDO> subs) {
        boolean firstFinish = !"FINISHED".equals(task.getStatus());
        if (firstFinish) {
            markTaskFinished(task);
            completionCoordinator.scheduleTaskCompletedOnly(task.getTaskId());
        }
        if (!allSurveySubsCaptureReady(subs)) {
            ingestFinishedSurveySubs(task, subs);
            log.info("task-center survey phase awaiting VTC capture taskId={}", task.getTaskId());
            return;
        }
        // 任务已 FINISHED 后重入（如修复核验复扫子任务轮询误触发）：只补未 ingest 的排查子任务，
        // 禁止重复交叉合并，避免将已处置(5)/已核验(6/7)实例误写回 stat=1 并落跃迁日志。
        if (!firstFinish) {
            ingestFinishedSurveySubs(task, subs);
            return;
        }
        finalizeSurveyPhaseArtifacts(task, subs);
    }

    private void finalizeSurveyPhaseArtifacts(OpenTaskDO task, List<OpenTaskSubDO> subs) {
        for (OpenTaskSubDO sub : subs) {
            ingestSurveySubIfNeeded(task, sub);
        }
        if (Boolean.TRUE.equals(task.getCrossScan())
                && !subs.isEmpty()
                && "vuln".equals(subs.get(0).getCenterTaskType())) {
            applyCrossScannerMerge(task, subs, TaskCenterSubSupport.PHASE_SURVEY);
        }
        completionCoordinator.scheduleExportAssembly(task.getTaskId());
    }

    private void onSurveyPhaseComplete(OpenTaskDO task, List<OpenTaskSubDO> subs) {
        handleSurveyPhaseTerminal(task, subs);
    }

    /**
     * 历史任务兼容：曾下发过 scan_phase=2 验证子任务的任务仍走此路径。
     */
    private void onVerifyPhaseComplete(OpenTaskDO task, List<OpenTaskSubDO> subs) {
        applyCrossScannerMerge(task, subs, TaskCenterSubSupport.PHASE_VERIFY);
        markTaskFinished(task);
        exportAssemblyDomainService.assembleForVerifyScan(task.getPartnerId(), task.getTaskId());
        completionCoordinator.scheduleNotify(task.getTaskId());
    }

    /**
     * 多扫描器交叉合并：直接读取各子任务已落库的排查结果，不二次下发 VTC。
     */
    private void applyCrossScannerMerge(OpenTaskDO task, List<OpenTaskSubDO> subs, int auditScanPhase) {
        if (subs == null || subs.isEmpty()) {
            return;
        }
        List<List<JSONObject>> perScanner = loadVulnResultsPerSub(subs);
        int totalScanners = subs.size();
        Map<String, Integer> hits = verifyMergeService.countScannerHits(perScanner);
        String strategy = resolveVerifyMergeStrategy(task);

        List<OpenVulnInstanceDO> existing = vulnInstanceRepository.listByPartnerAndTask(
                task.getPartnerId(), task.getTaskId(), null);
        Set<String> existingKeys = new HashSet<>();
        for (OpenVulnInstanceDO inst : existing) {
            JSONObject snap = snapshotOf(inst);
            existingKeys.add(verifyMergeService.dedupKey(snap));
        }

        String subId = subs.get(0).getSubId();
        applyMergeTransitions(task, existing, hits, totalScanners, strategy, subId, auditScanPhase);

        List<JSONObject> merged = TaskCenterVerifyStatusResolver.STRATEGY_UNION.equalsIgnoreCase(strategy)
                ? verifyMergeService.mergeUnion(perScanner)
                : verifyMergeService.merge(perScanner, false);
        List<JSONObject> newRows = new ArrayList<>();
        for (JSONObject row : merged) {
            String key = verifyMergeService.dedupKey(row);
            if (!existingKeys.contains(key)) {
                int stat = verifyStatusResolver.resolveVerifyStat(
                        hits.getOrDefault(key, 0), totalScanners, strategy);
                row.put("vulInfoStat", stat);
                newRows.add(row);
            }
        }
        if (!newRows.isEmpty()) {
            ingestCrossMergeNewInstances(task, newRows, subId, auditScanPhase);
        }

        log.info("task-center cross-scan merge taskId={} auditPhase={} strategy={} existing={} new={}",
                task.getTaskId(), auditScanPhase, strategy, existing.size(), newRows.size());
    }

    private void applyMergeTransitions(OpenTaskDO task,
                                       List<OpenVulnInstanceDO> instances,
                                       Map<String, Integer> scannerHits,
                                       int totalScanners,
                                       String strategy,
                                       String subId,
                                       int auditScanPhase) {
        if (CollectionUtils.isEmpty(instances)) {
            return;
        }
        for (OpenVulnInstanceDO inst : instances) {
            Integer prevStat = inst.getVulInfoStat();
            if (prevStat != null && prevStat > 3) {
                continue;
            }
            JSONObject snap = snapshotOf(inst);
            String key = verifyMergeService.dedupKey(snap);
            int hitCount = scannerHits.getOrDefault(key, 0);
            int newStat = verifyStatusResolver.resolveVerifyStat(hitCount, totalScanners, strategy);
            if (prevStat != null && prevStat == newStat) {
                continue;
            }
            OpenVulnInstanceAudit audit = auditScanPhase == TaskCenterSubSupport.PHASE_SURVEY
                    ? OpenVulnInstanceAudit.crossScanMerge(subId, strategy, hitCount)
                    : OpenVulnInstanceAudit.verifyPhase(subId, strategy, hitCount);
            OpenVulnInstanceAuditContext.runWith(
                    audit.taskId(task.getTaskId()),
                    () -> vulnInstanceRepository.updateState(inst.getId(), task.getPartnerId(), newStat, null, null));
        }
    }

    private List<List<JSONObject>> loadVulnResultsPerSub(List<OpenTaskSubDO> subs) {
        List<List<JSONObject>> perScanner = new ArrayList<>();
        for (OpenTaskSubDO sub : subs) {
            if (sub == null || !StringUtils.hasText(sub.getSubId())
                    || !scanResultQueryService.hasPersistedResults(sub.getSubId())) {
                perScanner.add(new ArrayList<>());
                continue;
            }
            TaskCenterSurveyBundle bundle = scanResultQueryService.loadSurveyBundleBySub(sub.getSubId());
            perScanner.add(resultsAdapter.toVulnInstances(bundle));
        }
        return perScanner;
    }

    private void ingestInstances(OpenTaskDO task, List<JSONObject> rows, int defaultStat,
                                 String subId, int scanPhase) {
        if (scanPhase == TaskCenterSubSupport.PHASE_SURVEY) {
            OpenTaskSubDO sub = openTaskSubRepository.findBySubId(subId);
            if (sub != null) {
                ingestSurveySubIfNeeded(task, sub);
            }
        } else {
            ingestVerifyNewInstances(task, rows, defaultStat, subId, scanPhase);
        }
    }

    private void markSubInstancesIngested(OpenTaskSubDO sub) {
        sub.setInstancesIngested(true);
        sub.setUpdatedAt(new Date());
        openTaskSubRepository.updateSub(sub);
    }

    private void syncTaskInstancesIngestedFlag(OpenTaskDO task) {
        List<OpenTaskSubDO> subs = openTaskSubRepository.listByTaskIdAndPhase(
                task.getTaskId(), TaskCenterSubSupport.PHASE_SURVEY);
        if (CollectionUtils.isEmpty(subs)) {
            return;
        }
        boolean allIngested = true;
        for (OpenTaskSubDO sub : subs) {
            if (sub == null || !TaskCenterSubSupport.STATUS_FINISHED.equals(sub.getStatus())) {
                allIngested = false;
                break;
            }
            if ("vuln".equals(sub.getCenterTaskType()) && !Boolean.TRUE.equals(sub.getInstancesIngested())) {
                allIngested = false;
                break;
            }
        }
        if (allIngested) {
            task.setInstancesIngested(true);
            task.setIngestError(null);
            task.setUpdatedAt(new Date());
            openTaskRepository.updateById(task);
        }
    }

    private void ingestSurveyInstances(OpenTaskDO task, List<JSONObject> rows, int defaultStat,
                                       String subId, int scanPhase) {
        OpenTaskSubDO sub = openTaskSubRepository.findBySubId(subId);
        if (sub != null) {
            ingestSurveySubIfNeeded(task, sub);
        }
    }

    private void ingestVerifyNewInstances(OpenTaskDO task, List<JSONObject> rows, int defaultStat,
                                          String subId, int scanPhase) {
        List<OpenVulnInstanceDO> persistRows = buildPersistRows(task, null, rows, defaultStat);
        if (persistRows.isEmpty()) {
            return;
        }
        vulnInstanceRepository.batchInsert(persistRows);
        instanceLogWriter.writeIngestBatch(task, persistRows, subId, scanPhase,
                OpenVulnInstanceLogDO.REASON_VERIFY_PHASE);
        log.info("task-center verify ingest new instances taskId={} count={}", task.getTaskId(), persistRows.size());
    }

    private void ingestCrossMergeNewInstances(OpenTaskDO task, List<JSONObject> rows, String subId, int scanPhase) {
        List<OpenVulnInstanceDO> persistRows = buildPersistRows(task, null, rows, 1);
        if (persistRows.isEmpty()) {
            return;
        }
        for (OpenVulnInstanceDO row : persistRows) {
            if (row.getVulInfoStat() == null) {
                row.setVulInfoStat(1);
            }
        }
        vulnInstanceRepository.batchInsert(persistRows);
        String reason = scanPhase == TaskCenterSubSupport.PHASE_SURVEY
                ? OpenVulnInstanceLogDO.REASON_CROSS_SCAN_MERGE
                : OpenVulnInstanceLogDO.REASON_VERIFY_PHASE;
        instanceLogWriter.writeIngestBatch(task, persistRows, subId, scanPhase, reason);
        log.info("task-center cross-merge ingest new instances taskId={} scanPhase={} count={}",
                task.getTaskId(), scanPhase, persistRows.size());
    }

    private List<OpenVulnInstanceDO> buildPersistRows(OpenTaskDO task, OpenTaskSubDO sub,
                                                    List<JSONObject> rows, int defaultStat) {
        Date now = new Date();
        List<OpenVulnInstanceDO> persistRows = new ArrayList<>();
        int seq = vulnInstanceRepository.listByPartnerAndTask(task.getPartnerId(), task.getTaskId(), null).size();
        for (JSONObject inst : rows) {
            seq++;
            JSONObject snap = JSONObject.parseObject(inst.toJSONString());
            if (snap.getInteger("vulInfoStat") == null) {
                snap.put("vulInfoStat", defaultStat);
            }
            if (sub != null) {
                snap.put("openSubId", sub.getSubId());
                if (StringUtils.hasText(sub.getScannerType())) {
                    snap.put("scannerType", sub.getScannerType());
                }
            }
            String vulInfoId = buildVulInfoId(task.getTaskId(), sub, snap, seq);
            snap.put("vulInfoID", vulInfoId);
            snap.put("vulInfoId", vulInfoId);

            OpenVulnInstanceDO row = new OpenVulnInstanceDO();
            row.setPartnerId(task.getPartnerId());
            row.setTaskId(task.getTaskId());
            row.setExtTaskId(task.getExtTaskId());
            row.setEngineTaskId(task.getEngineTaskId());
            row.setScanTemplateId(task.getScanTemplateId());
            row.setReportTemplateId(task.getReportTemplateId());
            row.setIngestStatus("SUCCESS");
            row.setIngestAt(now);
            row.setVulInfoId(vulInfoId);
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            InstanceItemConverter.toPersistRow(row, snap);
            persistRows.add(row);
        }
        return persistRows;
    }

    private static String resolveVerifyMergeStrategy(OpenTaskDO task) {
        if (task != null && StringUtils.hasText(task.getVerifyMergeStrategy())) {
            return task.getVerifyMergeStrategy().trim();
        }
        return TaskCenterScannerPlanner.resolveVerifyMergeStrategy(task != null ? task.getScanTemplateId() : null);
    }

    private static JSONObject snapshotOf(OpenVulnInstanceDO inst) {
        return StringUtils.hasText(inst.getSnapshotJson())
                ? JSONObject.parseObject(inst.getSnapshotJson()) : new JSONObject();
    }

    private boolean allSurveySubsCaptureReady(List<OpenTaskSubDO> subs) {
        if (CollectionUtils.isEmpty(subs)) {
            return true;
        }
        for (OpenTaskSubDO sub : subs) {
            if (sub == null || !TaskCenterSubSupport.STATUS_FINISHED.equals(sub.getStatus())) {
                continue;
            }
            if (scanResultQueryService.hasPersistedResults(sub.getSubId())) {
                continue;
            }
            if (captureRetryPolicy.exceededMaxWait(sub) || !captureRetryPolicy.isEnabled()) {
                continue;
            }
            return false;
        }
        return true;
    }

    private void markTaskFinished(OpenTaskDO task) {
        task.setStatus("FINISHED");
        task.setProgress(100);
        task.setFinishedAt(new Date());
        task.setUpdatedAt(new Date());
        openTaskRepository.updateById(task);
        operationCaseDomainService.onTaskScanTerminal(task);
    }

    private void markTaskFailed(OpenTaskDO task, String error) {
        task.setStatus("FAILED");
        task.setErrorMessage(error);
        task.setFinishedAt(new Date());
        task.setUpdatedAt(new Date());
        openTaskRepository.updateById(task);
        operationCaseDomainService.onTaskScanTerminal(task);
    }

    private void updateTaskProgress(OpenTaskDO task, List<OpenTaskSubDO> subs) {
        int sum = 0;
        int count = 0;
        for (OpenTaskSubDO sub : subs) {
            if (sub.getProgress() != null) {
                sum += sub.getProgress();
                count++;
            }
        }
        int progress = count > 0 ? Math.min(99, sum / count) : 0;
        if (!Integer.valueOf(progress).equals(task.getProgress())) {
            task.setProgress(progress);
            task.setStatus("RUNNING");
            task.setUpdatedAt(new Date());
            openTaskRepository.updateById(task);
        }
    }

    private static boolean allTerminal(List<OpenTaskSubDO> subs) {
        for (OpenTaskSubDO sub : subs) {
            if (!TaskCenterSubSupport.STATUS_FINISHED.equals(sub.getStatus())
                    && !TaskCenterSubSupport.STATUS_FAILED.equals(sub.getStatus())) {
                return false;
            }
        }
        return true;
    }

    private static boolean anyFailed(List<OpenTaskSubDO> subs) {
        for (OpenTaskSubDO sub : subs) {
            if (TaskCenterSubSupport.STATUS_FAILED.equals(sub.getStatus())) {
                return true;
            }
        }
        return false;
    }

    private static List<JSONObject> flatten(List<List<JSONObject>> perScanner) {
        List<JSONObject> all = new ArrayList<>();
        for (List<JSONObject> part : perScanner) {
            if (part != null) {
                all.addAll(part);
            }
        }
        return all;
    }

    private static String buildVulInfoId(String taskId, OpenTaskSubDO sub, JSONObject snap, int seq) {
        String vulId = snap.getString("vulId");
        String ip = snap.getString("vulNetAddr");
        String port = snap.get("vulPort") != null ? snap.get("vulPort").toString() : "0";
        String suffix = String.format("%04d", seq);
        String scanner = sub != null && StringUtils.hasText(sub.getScannerType())
                ? sub.getScannerType() : null;
        if (StringUtils.hasText(vulId)) {
            if (StringUtils.hasText(scanner)) {
                return "VI-" + taskId.replace("TASK-", "") + "-" + scanner + "-" + vulId + "-" + port + "-" + suffix;
            }
            return "VI-" + taskId.replace("TASK-", "") + "-" + vulId + "-" + port + "-" + suffix;
        }
        return "VI-" + taskId.replace("TASK-", "") + "-" + (ip != null ? ip : "x") + "-" + suffix;
    }
}
