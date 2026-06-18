package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.infra.config.OpenApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterPollScheduler {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterPollScheduler.class);

    private final TaskCenterSubProgressService progressService;
    private final TaskCenterVerifyFixProgressService verifyFixProgressService;
    private final TaskCenterTaskOrchestrator taskOrchestrator;
    private final OpenApiProperties properties;

    public TaskCenterPollScheduler(TaskCenterSubProgressService progressService,
                                   TaskCenterVerifyFixProgressService verifyFixProgressService,
                                   TaskCenterTaskOrchestrator taskOrchestrator,
                                   OpenApiProperties properties) {
        this.progressService = progressService;
        this.verifyFixProgressService = verifyFixProgressService;
        this.taskOrchestrator = taskOrchestrator;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${open-api.task-center.poll-interval-ms:30000}")
    public void pollRunningTasks() {
        if (!properties.getTaskCenter().isPollEnabled()) {
            return;
        }
        try {
            taskOrchestrator.retryDispatchFailed();
            progressService.pollRunningSubs();
            verifyFixProgressService.pollActiveJobs();
        } catch (Exception ex) {
            log.warn("task-center poll failed: {}", ex.getMessage());
        }
    }
}
