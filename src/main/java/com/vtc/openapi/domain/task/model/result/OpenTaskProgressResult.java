package com.vtc.openapi.domain.task.model.result;

import lombok.Data;

@Data
public class OpenTaskProgressResult {

    private String extTaskId;

    private String taskId;

    private String status;

    private Integer progress;

    private String startedAt;

    private String finishedAt;

    private String errorMessage;
}
