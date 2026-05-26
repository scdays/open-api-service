package com.vtc.openapi.domain.task.model.result;

import lombok.Data;

@Data
public class OpenTaskCreatedResult {

    private String extTaskId;

    private String taskId;

    private String status;

    private String createdAt;
}
