package com.vtc.openapi.domain.task.model.event;

/**
 * 任务到达 FINISHED 后的异步副作用事件。
 * <ul>
 *   <li>{@code exportOnly=false}：仅推送 TASK_COMPLETED Webhook</li>
 *   <li>{@code exportOnly=true}：仅组装数据外发文件并推送 EXPORT_READY</li>
 * </ul>
 */
public class TaskFinishedNotificationEvent {

    private final String taskId;
  private final boolean exportOnly;

    public TaskFinishedNotificationEvent(String taskId) {
        this(taskId, false);
    }

    public TaskFinishedNotificationEvent(String taskId, boolean exportOnly) {
        this.taskId = taskId;
        this.exportOnly = exportOnly;
    }

    public String getTaskId() {
        return taskId;
    }

    public boolean isExportOnly() {
        return exportOnly;
    }
}
