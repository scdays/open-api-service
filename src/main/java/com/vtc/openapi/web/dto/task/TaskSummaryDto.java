package com.vtc.openapi.web.dto.task;

public class TaskSummaryDto extends TaskProgressDto {

    private String taskName;
    private String createdAt;

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
