package com.vtc.openapi.app.service;

import com.vtc.openapi.web.dto.ApiResponse;
import com.vtc.openapi.web.dto.task.CreateTaskRequest;
import com.vtc.openapi.web.dto.task.CreateTaskResponse;
import com.vtc.openapi.web.dto.task.TaskListPageDto;
import com.vtc.openapi.web.dto.task.TaskProgressDto;

/**
 * 开放平台任务应用服务（P0）。
 */
public interface IOpenTaskAppService {

    ApiResponse<CreateTaskResponse> createTask(CreateTaskRequest request);

    ApiResponse<TaskProgressDto> getTask(String taskId);

    ApiResponse<TaskListPageDto> listTasks(String extTaskId, String status,
                                           String createdFrom, String createdTo, int page, int size);
}
