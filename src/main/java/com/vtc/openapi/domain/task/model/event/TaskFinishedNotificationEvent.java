package com.vtc.openapi.domain.task.model.event;

/**
 * Published when a mock task reaches FINISHED and platform side-effects should run.
 */
public class TaskFinishedNotificationEvent {

    private final String taskId;

    public TaskFinishedNotificationEvent(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }
}
