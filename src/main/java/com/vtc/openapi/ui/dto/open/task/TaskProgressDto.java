package com.vtc.openapi.ui.dto.open.task;

public class TaskProgressDto {

    private String extTaskId;
    private String taskId;
    private String status;
    private Integer progress;
    private String startedAt;
    private String finishedAt;
    private String errorMessage;

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

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
