package com.vtc.openapi.infra.adapter.taskcenter;

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
 * 子任务回收协调：task_finish 与 download_report_finish 可能乱序到达，
 * 在「子任务 FINISHED + 原始报告已归档」后再拉取 VTC 结果并推进编排 / 回调。
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

    public TaskCenterSubRecycleCoordinator(IOpenTaskSubRepository openTaskSubRepository,
                                           IOpenTaskRepository openTaskRepository,
                                           TaskCenterReportArchiveService reportArchiveService,
                                           TaskCenterSurveyRefetchService surveyRefetchService,
                                           TaskCenterRecycleService recycleService,
                                           TaskCenterScanResultQueryService scanResultQueryService,
                                           TaskCenterVerifyFixProgressService verifyFixProgressService) {
        this.openTaskSubRepository = openTaskSubRepository;
        this.openTaskRepository = openTaskRepository;
        this.reportArchiveService = reportArchiveService;
        this.surveyRefetchService = surveyRefetchService;
        this.recycleService = recycleService;
        this.scanResultQueryService = scanResultQueryService;
        this.verifyFixProgressService = verifyFixProgressService;
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
        if (sub.getScanPhase() != null && sub.getScanPhase() == TaskCenterSubSupport.PHASE_VERIFY_FIX) {
            verifyFixProgressService.tryCompleteAfterReportArchived(sub);
            return;
        }
        if (sub.getScanPhase() != null && sub.getScanPhase() == TaskCenterSubSupport.PHASE_VERIFY) {
            OpenTaskDO task = openTaskRepository.findByTaskId(sub.getTaskId());
            if (task != null) {
                recycleService.tryAdvanceTask(task);
            }
            return;
        }
        if (!StringUtils.hasText(sub.getSurveyId())) {
            log.debug("task-center sub recycle waiting surveyId subId={}", sub.getSubId());
            return;
        }
        if (!reportArchiveService.ensureArchived(sub)) {
            return;
        }
        OpenTaskSubDO latest = openTaskSubRepository.findBySubId(sub.getSubId());
        if (latest == null) {
            return;
        }
        if (scanResultQueryService.hasPersistedResults(latest.getSubId())) {
            OpenTaskDO task = openTaskRepository.findByTaskId(latest.getTaskId());
            if (task != null) {
                recycleService.ingestSurveySubIfNeeded(task, latest);
                recycleService.tryAdvanceTask(task);
            }
            return;
        }
        surveyRefetchService.captureOnSubFinished(latest);
        log.info("task-center sub recycle captured taskId={} subId={}", latest.getTaskId(), latest.getSubId());
    }

    public void retryPendingRecycle() {
        List<OpenTaskSubDO> pending = openTaskSubRepository.listFinishedAwaitingReportArchive(50);
        for (OpenTaskSubDO sub : pending) {
            tryRecycleSub(sub.getSubId());
        }
    }
}
