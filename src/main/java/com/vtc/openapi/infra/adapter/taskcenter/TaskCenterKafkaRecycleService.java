package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskSubRepository;
import com.vtc.openapi.infra.feign.dto.taskcenter.TaskFinishKafkaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 消费 task_finish_topic：标记子扫描完成并推进 open_task 编排或修复核验比对。
 */
@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterKafkaRecycleService {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterKafkaRecycleService.class);

    private final IOpenTaskSubRepository openTaskSubRepository;
    private final TaskCenterVerifyFixProgressService verifyFixProgressService;
    private final TaskCenterSubRecycleCoordinator subRecycleCoordinator;

    public TaskCenterKafkaRecycleService(IOpenTaskSubRepository openTaskSubRepository,
                                         TaskCenterVerifyFixProgressService verifyFixProgressService,
                                         TaskCenterSubRecycleCoordinator subRecycleCoordinator) {
        this.openTaskSubRepository = openTaskSubRepository;
        this.verifyFixProgressService = verifyFixProgressService;
        this.subRecycleCoordinator = subRecycleCoordinator;
    }

    @Transactional(rollbackFor = Exception.class)
    public void onTaskFinish(TaskFinishKafkaEvent event) {
        if (event == null || !StringUtils.hasText(event.getExtTaskId())) {
            return;
        }
        String openSubId = TaskCenterSocKeys.parseOpenSubId(event.getExtTaskId());
        if (!StringUtils.hasText(openSubId)) {
            log.warn("task-center kafka: unknown extTaskId={}", event.getExtTaskId());
            return;
        }
        if (openSubId.startsWith("VFS-")) {
            verifyFixProgressService.onLegacyKafkaTaskFinished(
                    openSubId, event.getTaskId(), event.getSurveyId());
            return;
        }
        OpenTaskSubDO sub = openTaskSubRepository.findBySubId(openSubId);
        if (sub == null) {
            log.warn("task-center kafka: sub not found extTaskId={} subId={}",
                    event.getExtTaskId(), openSubId);
            return;
        }
        onOpenTaskSubFinished(sub, event);
    }

    private void onOpenTaskSubFinished(OpenTaskSubDO sub, TaskFinishKafkaEvent event) {
        if (sub.getScanPhase() != null && sub.getScanPhase() == TaskCenterSubSupport.PHASE_VERIFY_FIX) {
            applySubFinishedState(sub, event);
            subRecycleCoordinator.tryRecycleSub(sub.getSubId());
            log.info("task-center kafka verify-fix sub finished jobId={} subId={} surveyId={}",
                    sub.getVerifyFixJobId(), sub.getSubId(), event.getSurveyId());
            return;
        }
        boolean alreadyFinished = TaskCenterSubSupport.STATUS_FINISHED.equals(sub.getStatus())
                && StringUtils.hasText(sub.getSurveyId())
                && sub.getSurveyId().equals(event.getSurveyId());
        if (!alreadyFinished) {
            applySubFinishedState(sub, event);
        }
        subRecycleCoordinator.tryRecycleSub(sub.getSubId());
        log.info("task-center kafka sub finished taskId={} subId={} surveyId={}",
                sub.getTaskId(), sub.getSubId(), sub.getSurveyId());
    }

    private void applySubFinishedState(OpenTaskSubDO sub, TaskFinishKafkaEvent event) {
        if (StringUtils.hasText(event.getSurveyId())) {
            sub.setSurveyId(event.getSurveyId());
        }
        if (StringUtils.hasText(event.getTaskId()) && !StringUtils.hasText(sub.getCenterPlanId())) {
            sub.setCenterPlanId(event.getTaskId());
        }
        sub.setStatus(TaskCenterSubSupport.STATUS_FINISHED);
        sub.setProgress(100);
        sub.setUpdatedAt(new Date());
        openTaskSubRepository.updateSub(sub);
    }
}
