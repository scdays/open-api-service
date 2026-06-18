package com.vtc.openapi.infra.adapter.taskcenter;

import lombok.Data;

@Data
public class TaskCenterDispatchRetryResult {

    private String taskId;
    private boolean success;
    private String message;
    private String taskStatus;
    private int retriedCount;
    private int successCount;
    private int failedCount;
}
