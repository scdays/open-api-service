package com.vtc.openapi.app.service;

import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.open.task.CreateScanTaskByFileRequest;
import com.vtc.openapi.ui.dto.open.task.CreateScanTaskByJsonRequest;
import com.vtc.openapi.ui.dto.open.task.CreateTaskResponse;
import com.vtc.openapi.ui.dto.open.task.TaskListPageDto;
import com.vtc.openapi.ui.dto.open.task.TaskProgressDto;
import org.springframework.web.multipart.MultipartFile;

/**
 * 开放平台任务应用服务（P0 · F0 契约对齐）。
 */
public interface IOpenTaskAppService {

    ApiResponse<CreateTaskResponse> createTaskByJson(CreateScanTaskByJsonRequest request);

    /**
     * 创建扫描任务（JSON body 内联转义 XML 字符串）。
     *
     * @deprecated 建议改用 {@link #createTaskByUpload(MultipartFile, String, Integer)}，
     * 后者以 multipart 文件上传提交 XML，免转义、对接更友好。本方法保留兼容存量调用方。
     */
    @Deprecated
    ApiResponse<CreateTaskResponse> createTaskByFile(CreateScanTaskByFileRequest request);

    /**
     * 创建扫描任务（multipart 文件上传 XML 报文）。
     *
     * @param file  XML 报文文件（根元素 {@code <scanTask>}，UTF-8）
     * @param extTaskId Partner 幂等键
     * @param type  任务类型（1/2/3，见附录 F）
     */
    ApiResponse<CreateTaskResponse> createTaskByUpload(MultipartFile file, String extTaskId, Integer type);

    ApiResponse<TaskProgressDto> getTask(String taskId);

    ApiResponse<TaskListPageDto> listTasks(String extTaskId, String status,
                                           String createdFrom, String createdTo, int page, int size);
}
