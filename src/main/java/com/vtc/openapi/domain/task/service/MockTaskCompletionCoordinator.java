package com.vtc.openapi.domain.task.service;

import com.vtc.openapi.domain.export.service.business.IExportAssemblyDomainService;
import com.vtc.openapi.domain.open.model.entity.WebhookDeliveryLogDO;
import com.vtc.openapi.domain.open.model.support.WebhookDeliverySupport;
import com.vtc.openapi.domain.open.repository.IApiInvocationRepository;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.event.TaskFinishedNotificationEvent;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.domain.webhook.model.WebhookEventType;
import com.vtc.openapi.domain.webhook.service.business.IWebhookPublishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Ensures TASK_COMPLETED webhook and TASK_COMPLETED export run after DB commit
 * (manual import + auto mock finish).
 */
@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "mock")
public class MockTaskCompletionCoordinator {

    private static final Logger log = LoggerFactory.getLogger(MockTaskCompletionCoordinator.class);

    private final ApplicationEventPublisher eventPublisher;
    private final IOpenTaskRepository openTaskRepository;
    private final IApiInvocationRepository apiInvocationRepository;
    private final IWebhookPublishService webhookPublishService;
    private final IExportAssemblyDomainService exportAssemblyDomainService;

    public MockTaskCompletionCoordinator(ApplicationEventPublisher eventPublisher,
                                         IOpenTaskRepository openTaskRepository,
                                         IApiInvocationRepository apiInvocationRepository,
                                         IWebhookPublishService webhookPublishService,
                                         IExportAssemblyDomainService exportAssemblyDomainService) {
        this.eventPublisher = eventPublisher;
        this.openTaskRepository = openTaskRepository;
        this.apiInvocationRepository = apiInvocationRepository;
        this.webhookPublishService = webhookPublishService;
        this.exportAssemblyDomainService = exportAssemblyDomainService;
    }

    public void scheduleNotify(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            return;
        }
        eventPublisher.publishEvent(new TaskFinishedNotificationEvent(taskId.trim()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTaskFinished(TaskFinishedNotificationEvent event) {
        if (event == null || !StringUtils.hasText(event.getTaskId())) {
            return;
        }
        String taskId = event.getTaskId().trim();
        synchronized (("task-finished-" + taskId).intern()) {
            OpenTaskDO task = openTaskRepository.findByTaskId(taskId);
            if (task == null || !"FINISHED".equals(task.getStatus())) {
                log.debug("skip task-finished side effects: taskId={} missing or not FINISHED", taskId);
                return;
            }
            if (!hasRecentTaskCompletedWebhook(task)) {
                webhookPublishService.publishTaskCompleted(task, null);
            } else {
                log.debug("skip duplicate TASK_COMPLETED webhook: taskId={}", task.getTaskId());
            }
            exportAssemblyDomainService.assembleForTaskCompleted(task);
        }
    }

    private boolean hasRecentTaskCompletedWebhook(OpenTaskDO task) {
        List<WebhookDeliveryLogDO> rows = apiInvocationRepository.listByResource(
                task.getPartnerId(), WebhookDeliverySupport.RESOURCE_TASK, task.getTaskId(), 20);
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        for (WebhookDeliveryLogDO row : rows) {
            if (WebhookEventType.TASK_COMPLETED.equals(row.getEventType())) {
                return true;
            }
        }
        return false;
    }
}
