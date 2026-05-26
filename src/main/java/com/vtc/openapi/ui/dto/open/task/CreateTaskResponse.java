package com.vtc.openapi.ui.dto.open.task;

public class CreateTaskResponse {

    private String extTaskId;
    private String taskId;
    private String status;
    private String createdAt;
    private String message;

    public String getExtTaskId() {
        return extTaskId;
    }

    public void setExtTaskId(String extTaskId) {
        this.extTaskId = extTaskId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
