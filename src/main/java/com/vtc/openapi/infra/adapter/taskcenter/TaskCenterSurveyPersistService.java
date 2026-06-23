package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskScanResultDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskScanResultRepository;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.infra.feign.dto.taskcenter.TaskCenterSurveyBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 子任务完成后从 VTC 拉取存活/端口/漏洞结果并落库（Kafka 或轮询回收触发，仅此时访问 VTC）。
 */
@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterSurveyPersistService {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterSurveyPersistService.class);

    private final IOpenTaskRepository openTaskRepository;
    private final IOpenTaskScanResultRepository scanResultRepository;
    private final TaskCenterSurveyFetchService surveyFetchService;
    private final TaskCenterExportRowBuilder exportRowBuilder;
    private final TaskCenterSurveyCaptureRetryPolicy captureRetryPolicy;

    public TaskCenterSurveyPersistService(IOpenTaskRepository openTaskRepository,
                                          IOpenTaskScanResultRepository scanResultRepository,
                                          TaskCenterSurveyFetchService surveyFetchService,
                                          TaskCenterExportRowBuilder exportRowBuilder,
                                          TaskCenterSurveyCaptureRetryPolicy captureRetryPolicy) {
        this.openTaskRepository = openTaskRepository;
        this.scanResultRepository = scanResultRepository;
        this.surveyFetchService = surveyFetchService;
        this.exportRowBuilder = exportRowBuilder;
        this.captureRetryPolicy = captureRetryPolicy;
    }

    @Transactional(rollbackFor = Exception.class)
    public SurveyPersistOutcome persistSubSurveyResults(OpenTaskSubDO sub) {
        if (sub == null || !StringUtils.hasText(sub.getSurveyId())) {
            return SurveyPersistOutcome.EMPTY_ACCEPTED;
        }
        OpenTaskDO task = openTaskRepository.findByTaskId(sub.getTaskId());
        if (task == null) {
            return SurveyPersistOutcome.EMPTY_ACCEPTED;
        }
        ScanTemplateSurveyScope scope = resolveSurveyScope(task, sub);
        TaskCenterSurveyBundle bundle = surveyFetchService.fetchAllWithRetry(sub.getSurveyId(), scope);
        List<String> taskHosts = TaskCenterExportRowBuilder.parseTaskHosts(task.getTargetsJson());
        List<OpenTaskScanResultDO> rows = exportRowBuilder.buildPersistRows(
                task, sub, bundle, taskHosts, new Date(), scope);
        if (rows.isEmpty()) {
            if (captureRetryPolicy.shouldDeferEmpty(sub, bundle)) {
                log.warn("task-center survey persist deferred (VTC lag) taskId={} subId={} surveyId={}",
                        sub.getTaskId(), sub.getSubId(), sub.getSurveyId());
                return SurveyPersistOutcome.DEFERRED_VTC_LAG;
            }
            log.info("task-center survey persist skipped empty bundle taskId={} subId={} surveyId={}",
                    sub.getTaskId(), sub.getSubId(), sub.getSurveyId());
            return SurveyPersistOutcome.EMPTY_ACCEPTED;
        }
        scanResultRepository.upsertBatch(rows);
        log.info("task-center survey persist ok taskId={} subId={} rows={}",
                sub.getTaskId(), sub.getSubId(), rows.size());
        return SurveyPersistOutcome.PERSISTED;
    }

    private static ScanTemplateSurveyScope resolveSurveyScope(OpenTaskDO task, OpenTaskSubDO sub) {
        if (sub != null && sub.getScanPhase() != null
                && sub.getScanPhase() == TaskCenterSubSupport.PHASE_VERIFY_FIX) {
            return ScanTemplateSurveyScope.vulnScanOnly();
        }
        if (sub != null && org.springframework.util.StringUtils.hasText(sub.getCenterTaskType())) {
            return ScanTemplateSurveyScope.fromCenterTaskType(sub.getCenterTaskType());
        }
        return ScanTemplateSurveyScope.fromScanTemplateId(task != null ? task.getScanTemplateId() : null);
    }
}
