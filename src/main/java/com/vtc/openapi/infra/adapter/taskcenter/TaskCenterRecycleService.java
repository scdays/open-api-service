package com.vtc.openapi.infra.adapter.taskcenter;

import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.instance.model.audit.OpenVulnInstanceAudit;
import com.vtc.openapi.domain.instance.model.audit.OpenVulnInstanceAuditContext;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceLogDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.domain.task.repository.IOpenTaskSubRepository;
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
    private final TaskCenterSurveyFetchService surveyFetchService;
    private final TaskCenterSurveyResultsAdapter resultsAdapter;
    private final TaskCenterVerifyMergeService verifyMergeService;
    private final TaskCenterVerifyStatusResolver verifyStatusResolver;
    private final TaskCenterTaskOrchestrator orchestrator;
    private final TaskCenterTaskCompletionCoordinator completionCoordinator;
    private final OpenVulnInstanceLogWriter instanceLogWriter;

    public TaskCenterRecycleService(IOpenTaskRepository openTaskRepository,
                                    IOpenTaskSubRepository openTaskSubRepository,
                                    IOpenVulnInstanceRepository vulnInstanceRepository,
                                    TaskCenterSurveyFetchService surveyFetchService,
                                    TaskCenterSurveyResultsAdapter resultsAdapter,
                                    TaskCenterVerifyMergeService verifyMergeService,
                                    TaskCenterVerifyStatusResolver verifyStatusResolver,
                                    TaskCenterTaskOrchestrator orchestrator,
                                    TaskCenterTaskCompletionCoordinator completionCoordinator,
                                    OpenVulnInstanceLogWriter instanceLogWriter) {
        this.openTaskRepository = openTaskRepository;
        this.openTaskSubRepository = openTaskSubRepository;
        this.vulnInstanceRepository = vulnInstanceRepository;
        this.surveyFetchService = surveyFetchService;
        this.resultsAdapter = resultsAdapter;
        this.verifyMergeService = verifyMergeService;
        this.verifyStatusResolver = verifyStatusResolver;
        this.orchestrator = orchestrator;
        this.completionCoordinator = completionCoordinator;
        this.instanceLogWriter = instanceLogWriter;
    }

    @Transactional(rollbackFor = Exception.class)
    public void tryAdvanceTask(OpenTaskDO task) {
        if (task == null || !StringUtils.hasText(task.getTaskId())) {
            return;
        }
        if ("FINISHED".equals(task.getStatus()) || "FAILED".equals(task.getStatus())
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
            updateTaskProgress(task, subs);
            return;
        }
        if (anyFailed(subs)) {
            markTaskFailed(task, "子扫描任务失败");
            return;
        }
        if (phase == TaskCenterSubSupport.PHASE_SURVEY) {
            onSurveyPhaseComplete(task, subs);
        } else if (phase == TaskCenterSubSupport.PHASE_VERIFY) {
            onVerifyPhaseComplete(task, subs);
        }
    }

    private void onSurveyPhaseComplete(OpenTaskDO task, List<OpenTaskSubDO> subs) {
        List<List<JSONObject>> perScanner = loadVulnResultsPerSub(subs);
        List<JSONObject> merged = Boolean.TRUE.equals(task.getCrossScan())
                ? verifyMergeService.mergeUnion(perScanner)
                : flatten(perScanner);
        if ("vuln".equals(subs.get(0).getCenterTaskType()) && !merged.isEmpty()) {
            ingestInstances(task, merged, 1, subs.get(0).getSubId(), TaskCenterSubSupport.PHASE_SURVEY);
        }
        boolean autoVerify = task.getAutoVerify() == null || Boolean.TRUE.equals(task.getAutoVerify());
        if (autoVerify && Boolean.TRUE.equals(task.getCrossScan()) && "vuln".equals(subs.get(0).getCenterTaskType())) {
            orchestrator.dispatchVerifyPhase(task);
            return;
        }
        markTaskFinished(task);
        completionCoordinator.scheduleNotify(task.getTaskId());
    }

    private void onVerifyPhaseComplete(OpenTaskDO task, List<OpenTaskSubDO> subs) {
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
        applyVerifyTransitions(task, existing, hits, totalScanners, strategy, subId);

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
            ingestInstances(task, newRows, 1, subId, TaskCenterSubSupport.PHASE_VERIFY);
        }

        markTaskFinished(task);
        completionCoordinator.scheduleNotify(task.getTaskId());
        log.info("task-center verify complete taskId={} strategy={} updated={} new={}",
                task.getTaskId(), strategy, existing.size(), newRows.size());
    }

    private void applyVerifyTransitions(OpenTaskDO task,
                                        List<OpenVulnInstanceDO> instances,
                                        Map<String, Integer> scannerHits,
                                        int totalScanners,
                                        String strategy,
                                        String subId) {
        if (CollectionUtils.isEmpty(instances)) {
            return;
        }
        for (OpenVulnInstanceDO inst : instances) {
            JSONObject snap = snapshotOf(inst);
            String key = verifyMergeService.dedupKey(snap);
            int hits = scannerHits.getOrDefault(key, 0);
            int newStat = verifyStatusResolver.resolveVerifyStat(hits, totalScanners, strategy);
            Integer prevStat = inst.getVulInfoStat();
            if (prevStat != null && prevStat == newStat) {
                continue;
            }
            OpenVulnInstanceAuditContext.runWith(
                    OpenVulnInstanceAudit.verifyPhase(subId, strategy, hits)
                            .taskId(task.getTaskId()),
                    () -> vulnInstanceRepository.updateState(inst.getId(), task.getPartnerId(), newStat, null, null));
        }
    }

    private List<List<JSONObject>> loadVulnResultsPerSub(List<OpenTaskSubDO> subs) {
        List<List<JSONObject>> perScanner = new ArrayList<>();
        for (OpenTaskSubDO sub : subs) {
            if (!StringUtils.hasText(sub.getSurveyId())) {
                perScanner.add(new ArrayList<>());
                continue;
            }
            TaskCenterSurveyBundle bundle = surveyFetchService.fetchAll(sub.getSurveyId());
            perScanner.add(resultsAdapter.toVulnInstances(bundle));
        }
        return perScanner;
    }

    private void ingestInstances(OpenTaskDO task, List<JSONObject> rows, int defaultStat,
                                 String subId, int scanPhase) {
        if (scanPhase == TaskCenterSubSupport.PHASE_SURVEY) {
            ingestSurveyInstances(task, rows, defaultStat, subId, scanPhase);
        } else {
            ingestVerifyNewInstances(task, rows, defaultStat, subId, scanPhase);
        }
    }

    private void ingestSurveyInstances(OpenTaskDO task, List<JSONObject> rows, int defaultStat,
                                       String subId, int scanPhase) {
        if (Boolean.TRUE.equals(task.getInstancesIngested())) {
            return;
        }
        if (vulnInstanceRepository.existsByPartnerAndTaskId(task.getPartnerId(), task.getTaskId())) {
            task.setInstancesIngested(true);
            task.setUpdatedAt(new Date());
            openTaskRepository.updateById(task);
            return;
        }
        List<OpenVulnInstanceDO> persistRows = buildPersistRows(task, rows, defaultStat);
        if (!persistRows.isEmpty()) {
            vulnInstanceRepository.batchInsert(persistRows);
            instanceLogWriter.writeIngestBatch(task, persistRows, subId, scanPhase);
        }
        task.setInstancesIngested(true);
        task.setIngestError(null);
        task.setUpdatedAt(new Date());
        openTaskRepository.updateById(task);
        log.info("task-center survey ingest finished taskId={} count={}", task.getTaskId(), persistRows.size());
    }

    private void ingestVerifyNewInstances(OpenTaskDO task, List<JSONObject> rows, int defaultStat,
                                          String subId, int scanPhase) {
        List<OpenVulnInstanceDO> persistRows = buildPersistRows(task, rows, defaultStat);
        if (persistRows.isEmpty()) {
            return;
        }
        vulnInstanceRepository.batchInsert(persistRows);
        instanceLogWriter.writeIngestBatch(task, persistRows, subId, scanPhase,
                OpenVulnInstanceLogDO.REASON_VERIFY_PHASE);
        log.info("task-center verify ingest new instances taskId={} count={}", task.getTaskId(), persistRows.size());
    }

    private List<OpenVulnInstanceDO> buildPersistRows(OpenTaskDO task, List<JSONObject> rows, int defaultStat) {
        Date now = new Date();
        List<OpenVulnInstanceDO> persistRows = new ArrayList<>();
        int seq = vulnInstanceRepository.listByPartnerAndTask(task.getPartnerId(), task.getTaskId(), null).size();
        for (JSONObject inst : rows) {
            seq++;
            JSONObject snap = JSONObject.parseObject(inst.toJSONString());
            if (snap.getInteger("vulInfoStat") == null) {
                snap.put("vulInfoStat", defaultStat);
            }
            String vulInfoId = buildVulInfoId(task.getTaskId(), snap, seq);
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

    private void markTaskFinished(OpenTaskDO task) {
        task.setStatus("FINISHED");
        task.setProgress(100);
        task.setFinishedAt(new Date());
        task.setUpdatedAt(new Date());
        openTaskRepository.updateById(task);
    }

    private void markTaskFailed(OpenTaskDO task, String error) {
        task.setStatus("FAILED");
        task.setErrorMessage(error);
        task.setFinishedAt(new Date());
        task.setUpdatedAt(new Date());
        openTaskRepository.updateById(task);
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

    private static String buildVulInfoId(String taskId, JSONObject snap, int seq) {
        String vulId = snap.getString("vulId");
        String ip = snap.getString("vulNetAddr");
        String port = snap.get("vulPort") != null ? snap.get("vulPort").toString() : "0";
        String suffix = String.format("%04d", seq);
        if (StringUtils.hasText(vulId)) {
            return "VI-" + taskId.replace("TASK-", "") + "-" + vulId + "-" + port + "-" + suffix;
        }
        return "VI-" + taskId.replace("TASK-", "") + "-" + (ip != null ? ip : "x") + "-" + suffix;
    }
}
