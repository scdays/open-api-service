package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceLogRepository;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.domain.task.repository.IOpenTaskScanResultRepository;
import com.vtc.openapi.domain.task.repository.IOpenTaskSubRepository;
import com.vtc.openapi.ui.dto.admin.OpenTaskSurveyRefetchResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 子任务扫描完成后从 VTC 拉取结果并落库、推进排查 ingest（Kafka / 轮询 / 运营手动触发共用）。
 * <p>二次获取时先清除该子任务扫描结果及该子任务排查 ingest 痕迹，保证幂等。</p>
 */
@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterSurveyRefetchService {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterSurveyRefetchService.class);

    private final IOpenTaskRepository openTaskRepository;
    private final IOpenTaskSubRepository openTaskSubRepository;
    private final IOpenTaskScanResultRepository scanResultRepository;
    private final IOpenVulnInstanceRepository vulnInstanceRepository;
    private final IOpenVulnInstanceLogRepository vulnInstanceLogRepository;
    private final TaskCenterSurveyPersistService surveyPersistService;
    private final TaskCenterRecycleService recycleService;

    public TaskCenterSurveyRefetchService(IOpenTaskRepository openTaskRepository,
                                          IOpenTaskSubRepository openTaskSubRepository,
                                          IOpenTaskScanResultRepository scanResultRepository,
                                          IOpenVulnInstanceRepository vulnInstanceRepository,
                                          IOpenVulnInstanceLogRepository vulnInstanceLogRepository,
                                          TaskCenterSurveyPersistService surveyPersistService,
                                          TaskCenterRecycleService recycleService) {
        this.openTaskRepository = openTaskRepository;
        this.openTaskSubRepository = openTaskSubRepository;
        this.scanResultRepository = scanResultRepository;
        this.vulnInstanceRepository = vulnInstanceRepository;
        this.vulnInstanceLogRepository = vulnInstanceLogRepository;
        this.surveyPersistService = surveyPersistService;
        this.recycleService = recycleService;
    }

    @Transactional(rollbackFor = Exception.class)
    public OpenTaskSurveyRefetchResultDto refetchSurveySub(String taskId, String subId) {
        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(subId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "taskId / subId 不能为空");
        }
        OpenTaskDO task = openTaskRepository.findByTaskId(taskId.trim());
        if (task == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "任务不存在");
        }
        OpenTaskSubDO sub = openTaskSubRepository.findBySubId(subId.trim());
        if (sub == null || !taskId.trim().equals(sub.getTaskId())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "子任务不存在或不属于该任务");
        }
        validateSurveySub(sub, task);
        return captureSurveySubResults(task, sub);
    }

    /**
     * Kafka / 轮询子任务完成时调用（等价于运营「重新获取」，含清除旧数据）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void captureOnSubFinished(OpenTaskSubDO sub) {
        if (sub == null || !StringUtils.hasText(sub.getTaskId()) || !StringUtils.hasText(sub.getSubId())) {
            return;
        }
        if (sub.getScanPhase() == null || sub.getScanPhase() != TaskCenterSubSupport.PHASE_SURVEY) {
            return;
        }
        if (!StringUtils.hasText(sub.getSurveyId())) {
            log.warn("survey capture skipped: missing surveyId taskId={} subId={}", sub.getTaskId(), sub.getSubId());
            return;
        }
        OpenTaskDO task = openTaskRepository.findByTaskId(sub.getTaskId());
        if (task == null) {
            return;
        }
        try {
            validateSurveySub(sub, task);
        } catch (OpenApiException ex) {
            log.warn("survey capture skipped taskId={} subId={}: {}", sub.getTaskId(), sub.getSubId(), ex.getMessage());
            return;
        }
        captureSurveySubResults(task, sub);
    }

    private OpenTaskSurveyRefetchResultDto captureSurveySubResults(OpenTaskDO task, OpenTaskSubDO sub) {
        OpenTaskSurveyRefetchResultDto dto = new OpenTaskSurveyRefetchResultDto();
        dto.setTaskId(task.getTaskId());
        dto.setSubId(sub.getSubId());

        int clearedScanRows = scanResultRepository.deleteBySubId(sub.getSubId());
        RollbackCounters rollback = rollbackSubSurveyIngest(task, sub);

        if (!TaskCenterSubSupport.STATUS_FINISHED.equals(sub.getStatus())) {
            sub.setStatus(TaskCenterSubSupport.STATUS_FINISHED);
            sub.setProgress(100);
            sub.setUpdatedAt(new Date());
            openTaskSubRepository.updateSub(sub);
        }

        SurveyPersistOutcome outcome = surveyPersistService.persistSubSurveyResults(sub);
        int persistedScanRows = scanResultRepository.listBySubId(sub.getSubId(), null).size();

        if (outcome == SurveyPersistOutcome.DEFERRED_VTC_LAG) {
            dto.setSuccess(false);
            dto.setPersistedScanRows(0);
            dto.setTaskStatus(task.getStatus());
            dto.setMessage("VTC 扫描结果尚未入库，已安排自动重试（task_finish 早于 VTC 落库）");
            log.warn("survey capture deferred VTC lag taskId={} subId={} surveyId={}",
                    task.getTaskId(), sub.getSubId(), sub.getSurveyId());
            return dto;
        }

        OpenTaskDO latest = openTaskRepository.findByTaskId(task.getTaskId());
        if (latest != null) {
            OpenTaskSubDO latestSub = openTaskSubRepository.findBySubId(sub.getSubId());
            if (latestSub != null) {
                recycleService.ingestSurveySubIfNeeded(latest, latestSub);
            }
            recycleService.tryAdvanceTask(latest);
            latest = openTaskRepository.findByTaskId(task.getTaskId());
        }

        dto.setSuccess(true);
        dto.setClearedScanRows(clearedScanRows);
        dto.setClearedInstances(rollback.clearedInstances);
        dto.setClearedSurveyLogs(rollback.clearedLogs);
        dto.setPersistedScanRows(persistedScanRows);
        dto.setTaskStatus(latest != null ? latest.getStatus() : task.getStatus());
        dto.setMessage(buildMessage(clearedScanRows, persistedScanRows, rollback));
        log.info("survey capture ok taskId={} subId={} clearedScan={} persistedScan={} clearedInst={}",
                task.getTaskId(), sub.getSubId(), clearedScanRows, persistedScanRows, rollback.clearedInstances);
        return dto;
    }

    private RollbackCounters rollbackSubSurveyIngest(OpenTaskDO task, OpenTaskSubDO sub) {
        RollbackCounters counters = new RollbackCounters();
        if (task.getTaskPhase() != null && task.getTaskPhase() > TaskCenterSubSupport.PHASE_SURVEY) {
            return counters;
        }
        List<String> vulInfoIds = vulnInstanceLogRepository.listVulInfoIdsByTaskSubAndPhase(
                task.getTaskId(), sub.getSubId(), TaskCenterSubSupport.PHASE_SURVEY);
        if (!vulInfoIds.isEmpty()) {
            counters.clearedInstances = vulnInstanceRepository.deleteByPartnerAndVulInfoIds(
                    task.getPartnerId(), vulInfoIds);
        }
        counters.clearedLogs = vulnInstanceLogRepository.deleteByTaskIdAndSubIdAndScanPhase(
                task.getTaskId(), sub.getSubId(), TaskCenterSubSupport.PHASE_SURVEY);
        sub.setInstancesIngested(false);
        sub.setUpdatedAt(new Date());
        openTaskSubRepository.updateSub(sub);
        if (counters.clearedInstances > 0 || counters.clearedLogs > 0) {
            task.setInstancesIngested(false);
            task.setIngestError(null);
            if ("FINISHED".equals(task.getStatus())) {
                task.setStatus("RUNNING");
                task.setFinishedAt(null);
            }
            task.setUpdatedAt(new Date());
            openTaskRepository.updateById(task);
        }
        return counters;
    }

    private static void validateSurveySub(OpenTaskSubDO sub, OpenTaskDO task) {
        if (sub.getScanPhase() == null || sub.getScanPhase() != TaskCenterSubSupport.PHASE_SURVEY) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "仅支持排查阶段子任务");
        }
        if (!StringUtils.hasText(sub.getSurveyId())) {
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID, "子任务尚无 surveyId，无法获取扫描结果");
        }
        if (task.getTaskPhase() != null && task.getTaskPhase() > TaskCenterSubSupport.PHASE_SURVEY) {
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID,
                    "任务已进入验证阶段，不支持重新获取排查结果");
        }
        if ("FAILED".equals(task.getStatus())
                || OpenApiConstants.TASK_DISPATCH_FAILED.equals(task.getStatus())) {
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID, "任务已失败，请先重试下发");
        }
    }

    private static String buildMessage(int clearedScan, int persistedScan, RollbackCounters rollback) {
        StringBuilder sb = new StringBuilder("已重新获取 VTC 扫描结果");
        if (clearedScan > 0) {
            sb.append("，清除原扫描结果 ").append(clearedScan).append(" 条");
        }
        if (rollback.clearedInstances > 0) {
            sb.append("，清除实例 ").append(rollback.clearedInstances).append(" 条");
        }
        if (rollback.clearedLogs > 0) {
            sb.append("，清除排查跃迁日志 ").append(rollback.clearedLogs).append(" 条");
        }
        sb.append("，本次落库 ").append(persistedScan).append(" 条");
        return sb.toString();
    }

    private static final class RollbackCounters {
        private int clearedInstances;
        private int clearedLogs;
    }
}
