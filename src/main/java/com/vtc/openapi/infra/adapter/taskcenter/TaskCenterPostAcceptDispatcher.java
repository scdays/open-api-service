package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * 任务受理落库后异步下发 VTC，失败不回滚、不阻断 Partner 创建响应。
 */
@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterPostAcceptDispatcher {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterPostAcceptDispatcher.class);

    private final IOpenTaskRepository openTaskRepository;
    private final TaskCenterTaskOrchestrator orchestrator;

    public TaskCenterPostAcceptDispatcher(IOpenTaskRepository openTaskRepository,
                                          TaskCenterTaskOrchestrator orchestrator) {
        this.openTaskRepository = openTaskRepository;
        this.orchestrator = orchestrator;
    }

    public void scheduleSurveyDispatch(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatchSurveyNow(taskId);
                }
            });
            return;
        }
        dispatchSurveyNow(taskId);
    }

    public void dispatchSurveyNow(String taskId) {
        try {
            OpenTaskDO task = openTaskRepository.findByTaskId(taskId);
            if (task == null) {
                log.warn("task-center dispatch skipped, task not found: {}", taskId);
                return;
            }
            orchestrator.dispatchSurveyPhase(task);
        } catch (Exception ex) {
            log.error("task-center survey dispatch unexpected error taskId={}", taskId, ex);
            markDispatchFailed(taskId, ex.getMessage());
        }
    }

    private void markDispatchFailed(String taskId, String message) {
        OpenTaskDO task = openTaskRepository.findByTaskId(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(OpenApiConstants.TASK_DISPATCH_FAILED);
        task.setErrorMessage(TaskCenterTaskOrchestrator.truncateError(message));
        task.setUpdatedAt(new java.util.Date());
        openTaskRepository.updateById(task);
    }
}
