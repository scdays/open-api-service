package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.domain.artifact.service.business.IArtifactWebhookCoordinator;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.domain.task.repository.IOpenTaskSubRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 子任务回收协调：扫描结果拉取与原始报告归档<strong>并行</strong>，互不阻塞。
 * <ul>
 *   <li>{@code task_finish_topic} → VTC Feign 拉取存活/端口/漏洞 → 落库 → ingest / 任务推进 / 数据外发</li>
 *   <li>{@code download_report_finish_topic} → 报告路径落库 → SFTP 归档 → 待发 ARTIFACT_READY</li>
 * </ul>
 * VTC 入库滞后时由 {@link TaskCenterSurveyCaptureRetryPolicy} 延迟重试，轮询补拉。
 */
@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterSubRecycleCoordinator {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterSubRecycleCoordinator.class);

    private final IOpenTaskSubRepository openTaskSubRepository;
    private final IOpenTaskRepository openTaskRepository;
    private final TaskCenterReportArchiveService reportArchiveService;
    private final TaskCenterSurveyRefetchService surveyRefetchService;
    private final TaskCenterRecycleService recycleService;
    private final TaskCenterScanResultQueryService scanResultQueryService;
    private final TaskCenterVerifyFixProgressService verifyFixProgressService;
    private final IArtifactWebhookCoordinator artifactWebhookCoordinator;

    public TaskCenterSubRecycleCoordinator(IOpenTaskSubRepository openTaskSubRepository,
                                           IOpenTaskRepository openTaskRepository,
                                           TaskCenterReportArchiveService reportArchiveService,
                                           TaskCenterSurveyRefetchService surveyRefetchService,
                                           TaskCenterRecycleService recycleService,
                                           TaskCenterScanResultQueryService scanResultQueryService,
                                           TaskCenterVerifyFixProgressService verifyFixProgressService,
                                           IArtifactWebhookCoordinator artifactWebhookCoordinator) {
        this.openTaskSubRepository = openTaskSubRepository;
        this.openTaskRepository = openTaskRepository;
        this.reportArchiveService = reportArchiveService;
        this.surveyRefetchService = surveyRefetchService;
        this.recycleService = recycleService;
        this.scanResultQueryService = scanResultQueryService;
        this.verifyFixProgressService = verifyFixProgressService;
        this.artifactWebhookCoordinator = artifactWebhookCoordinator;
    }

    @Transactional(rollbackFor = Exception.class)
    public void tryRecycleSub(String subId) {
        if (!StringUtils.hasText(subId)) {
            return;
        }
        OpenTaskSubDO sub = openTaskSubRepository.findBySubId(subId.trim());
        if (sub == null) {
            return;
        }
        if (!TaskCenterSubSupport.STATUS_FINISHED.equals(sub.getStatus())) {
            return;
        }

        // 报告归档：best-effort，不阻塞结果拉取
        tryArchiveReportBestEffort(sub);

        if (sub.getScanPhase() != null && sub.getScanPhase() == TaskCenterSubSupport.PHASE_VERIFY_FIX) {
            verifyFixProgressService.tryCaptureSurveyOnSubFinished(sub);
            return;
        }
        if (sub.getScanPhase() != null && sub.getScanPhase() == TaskCenterSubSupport.PHASE_VERIFY) {
            OpenTaskDO task = openTaskRepository.findByTaskId(sub.getTaskId());
            if (task != null) {
                recycleService.tryAdvanceTask(task);
            }
            return;
        }
        tryCaptureSurveySub(sub);
    }

    public void retryPendingRecycle() {
        List<OpenTaskSubDO> pendingArchive = openTaskSubRepository.listFinishedAwaitingReportArchive(50);
        for (OpenTaskSubDO sub : pendingArchive) {
            tryArchiveReportBestEffort(sub);
        }
        artifactWebhookCoordinator.retryPendingDeliveries(50);
        List<OpenTaskSubDO> pendingCapture = openTaskSubRepository.listFinishedAwaitingSurveyCapture(50);
        for (OpenTaskSubDO sub : pendingCapture) {
            if (scanResultQueryService.hasPersistedResults(sub.getSubId())) {
                continue;
            }
            OpenTaskDO task = openTaskRepository.findByTaskId(sub.getTaskId());
            if (task == null) {
                continue;
            }
            if (sub.getScanPhase() != null && sub.getScanPhase() == TaskCenterSubSupport.PHASE_VERIFY_FIX) {
                verifyFixProgressService.tryCaptureSurveyOnSubFinished(sub);
            } else if (sub.getScanPhase() != null && sub.getScanPhase() == TaskCenterSubSupport.PHASE_SURVEY) {
                tryCaptureSurveySub(sub);
            }
        }
        // 任务已 FINISHED 但外发尚未生成时，capture 完成后由 tryAdvanceTask 触发 EXPORT（仅排查子任务）
        for (OpenTaskSubDO sub : pendingCapture) {
            if (sub.getScanPhase() != null && sub.getScanPhase() == TaskCenterSubSupport.PHASE_VERIFY_FIX) {
                continue;
            }
            if (!scanResultQueryService.hasPersistedResults(sub.getSubId())) {
                continue;
            }
            OpenTaskDO task = openTaskRepository.findByTaskId(sub.getTaskId());
            if (task != null && "FINISHED".equals(task.getStatus())) {
                recycleService.tryAdvanceTask(task);
            }
        }
    }

    private void tryArchiveReportBestEffort(OpenTaskSubDO sub) {
        if (sub == null || !TaskCenterReportArchiveService.requiresRawReport(sub)) {
            return;
        }
        try {
            reportArchiveService.ensureArchived(sub);
        } catch (Exception ex) {
            log.warn("task-center report archive best-effort failed subId={}: {}",
                    sub.getSubId(), ex.getMessage());
        }
    }

    private void tryCaptureSurveySub(OpenTaskSubDO sub) {
        if (sub == null || !StringUtils.hasText(sub.getSurveyId())) {
            log.debug("task-center sub recycle waiting surveyId subId={}",
                    sub != null ? sub.getSubId() : null);
            return;
        }
        if (scanResultQueryService.hasPersistedResults(sub.getSubId())) {
            OpenTaskDO task = openTaskRepository.findByTaskId(sub.getTaskId());
            if (task != null) {
                recycleService.ingestSurveySubIfNeeded(task, sub);
                recycleService.tryAdvanceTask(task);
            }
            return;
        }
        surveyRefetchService.captureOnSubFinished(sub);
        log.info("task-center sub recycle captured taskId={} subId={}", sub.getTaskId(), sub.getSubId());
    }
}
