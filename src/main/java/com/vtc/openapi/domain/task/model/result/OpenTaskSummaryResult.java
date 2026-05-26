package com.vtc.openapi.domain.task.model.result;

import lombok.Data;

@Data
public class OpenTaskSummaryResult {

    private String extTaskId;

    private String taskId;

    private String taskName;

    private String status;

    private Integer progress;

    private String startedAt;

    private String finishedAt;

    private String errorMessage;

    private String createdAt;
}
