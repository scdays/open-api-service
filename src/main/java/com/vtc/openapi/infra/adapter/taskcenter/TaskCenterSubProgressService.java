package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.domain.open.OpenApiConstants;
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

import java.util.Date;
import java.util.List;

@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterSubProgressService {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterSubProgressService.class);

    private final IOpenTaskSubRepository openTaskSubRepository;
    private final IOpenTaskRepository openTaskRepository;
    private final TaskCenterSurveyResolver surveyResolver;
    private final TaskCenterRecycleService recycleService;
    private final TaskCenterSubRecycleCoordinator subRecycleCoordinator;

    public TaskCenterSubProgressService(IOpenTaskSubRepository openTaskSubRepository,
                                        IOpenTaskRepository openTaskRepository,
                                        TaskCenterSurveyResolver surveyResolver,
                                        TaskCenterRecycleService recycleService,
                                        TaskCenterSubRecycleCoordinator subRecycleCoordinator) {
        this.openTaskSubRepository = openTaskSubRepository;
        this.openTaskRepository = openTaskRepository;
        this.surveyResolver = surveyResolver;
        this.recycleService = recycleService;
        this.subRecycleCoordinator = subRecycleCoordinator;
    }

    @Transactional(rollbackFor = Exception.class)
    public void refreshTask(OpenTaskDO task) {
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
        for (OpenTaskSubDO sub : subs) {
            refreshSub(sub);
        }
        OpenTaskDO latest = openTaskRepository.findByTaskId(task.getTaskId());
        if (latest != null) {
            recycleService.tryAdvanceTask(latest);
        }
    }

    private void refreshSub(OpenTaskSubDO sub) {
        if (sub == null || TaskCenterSubSupport.STATUS_FINISHED.equals(sub.getStatus())
                || TaskCenterSubSupport.STATUS_FAILED.equals(sub.getStatus())) {
            return;
        }
        if (sub.getScanPhase() != null && sub.getScanPhase() == TaskCenterSubSupport.PHASE_VERIFY_FIX) {
            return;
        }
        if (!StringUtils.hasText(sub.getCenterPlanId())) {
            return;
        }
        TaskCenterSurveyResolver.SurveyPollResult poll = surveyResolver.pollSurvey(sub.getCenterPlanId());
        boolean changed = false;
        if (StringUtils.hasText(poll.getSurveyId()) && !poll.getSurveyId().equals(sub.getSurveyId())) {
            sub.setSurveyId(poll.getSurveyId());
            changed = true;
        }
        if (poll.getProgress() > 0 && (sub.getProgress() == null || poll.getProgress() > sub.getProgress())) {
            sub.setProgress(poll.getProgress());
            changed = true;
        }
        if (poll.isFinished()) {
            sub.setStatus(TaskCenterSubSupport.STATUS_FINISHED);
            sub.setProgress(100);
            changed = true;
            log.info("task-center sub finished taskId={} subId={} surveyId={}",
                    sub.getTaskId(), sub.getSubId(), sub.getSurveyId());
        } else if (TaskCenterSubSupport.STATUS_PENDING.equals(sub.getStatus())) {
            sub.setStatus(TaskCenterSubSupport.STATUS_RUNNING);
            changed = true;
        }
        if (changed) {
            sub.setUpdatedAt(new Date());
            openTaskSubRepository.updateSub(sub);
            if (TaskCenterSubSupport.STATUS_FINISHED.equals(sub.getStatus())) {
                if (sub.getScanPhase() != null && sub.getScanPhase() == TaskCenterSubSupport.PHASE_VERIFY) {
                    OpenTaskDO task = openTaskRepository.findByTaskId(sub.getTaskId());
                    if (task != null) {
                        recycleService.tryAdvanceTask(task);
                    }
                } else {
                    subRecycleCoordinator.tryRecycleSub(sub.getSubId());
                }
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void pollRunningSubs() {
        List<OpenTaskSubDO> running = openTaskSubRepository.listRunning();
        for (OpenTaskSubDO sub : running) {
            refreshSub(sub);
            // 修复核验子任务(phase=3)挂在原 open_task 下，不得误触发父任务排查 ingest/交叉合并
            if (sub.getScanPhase() != null && sub.getScanPhase() == TaskCenterSubSupport.PHASE_VERIFY_FIX) {
                continue;
            }
            OpenTaskDO task = openTaskRepository.findByTaskId(sub.getTaskId());
            if (task != null) {
                recycleService.tryAdvanceTask(task);
            }
        }
        subRecycleCoordinator.retryPendingRecycle();
    }
}
