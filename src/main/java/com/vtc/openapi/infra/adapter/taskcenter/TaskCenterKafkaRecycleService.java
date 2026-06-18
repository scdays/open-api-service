package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
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
 * 消费 task_finish_topic：标记子扫描完成并推进 open_task 编排。
 */
@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterKafkaRecycleService {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterKafkaRecycleService.class);

    private final IOpenTaskSubRepository openTaskSubRepository;
    private final IOpenTaskRepository openTaskRepository;
    private final TaskCenterVerifyFixProgressService verifyFixProgressService;
    private final TaskCenterSurveyRefetchService surveyRefetchService;
    private final TaskCenterRecycleService recycleService;

    public TaskCenterKafkaRecycleService(IOpenTaskSubRepository openTaskSubRepository,
                                         IOpenTaskRepository openTaskRepository,
                                         TaskCenterVerifyFixProgressService verifyFixProgressService,
                                         TaskCenterSurveyRefetchService surveyRefetchService,
                                         TaskCenterRecycleService recycleService) {
        this.openTaskSubRepository = openTaskSubRepository;
        this.openTaskRepository = openTaskRepository;
        this.verifyFixProgressService = verifyFixProgressService;
        this.surveyRefetchService = surveyRefetchService;
        this.recycleService = recycleService;
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
            verifyFixProgressService.onKafkaTaskFinished(openSubId, event.getTaskId(), event.getSurveyId());
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
        boolean alreadyFinished = TaskCenterSubSupport.STATUS_FINISHED.equals(sub.getStatus())
                && StringUtils.hasText(sub.getSurveyId())
                && sub.getSurveyId().equals(event.getSurveyId());
        if (!alreadyFinished) {
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
        if (sub.getScanPhase() != null && sub.getScanPhase() == TaskCenterSubSupport.PHASE_VERIFY) {
            OpenTaskDO task = openTaskRepository.findByTaskId(sub.getTaskId());
            if (task != null) {
                recycleService.tryAdvanceTask(task);
            }
            log.info("task-center kafka verify sub finished taskId={} subId={}", sub.getTaskId(), sub.getSubId());
            return;
        }
        surveyRefetchService.captureOnSubFinished(sub);
        log.info("task-center kafka sub finished taskId={} subId={} surveyId={}",
                sub.getTaskId(), sub.getSubId(), sub.getSurveyId());
    }
}
