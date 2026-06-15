package com.vtc.openapi.ui.open;

import com.vtc.openapi.app.service.IOpenTaskAppService;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.open.task.CreateScanTaskByFileRequest;
import com.vtc.openapi.ui.dto.open.task.CreateScanTaskByJsonRequest;
import com.vtc.openapi.ui.dto.open.task.CreateTaskResponse;
import com.vtc.openapi.ui.dto.open.task.TaskListPageDto;
import com.vtc.openapi.ui.dto.open.task.TaskProgressDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 开放平台任务 REST（/api/open/v1/tasks · P0 · F0 契约对齐）。
 */
@RestController
@RequestMapping(OpenApiConstants.API_PREFIX)
@Api(tags = "开放平台 · 任务")
public class OpenTaskUI {

    private final IOpenTaskAppService openTaskAppService;

    public OpenTaskUI(IOpenTaskAppService openTaskAppService) {
        this.openTaskAppService = openTaskAppService;
    }

    @ApiOperation(value = "创建扫描任务（JSON）", notes = "§5.1.2 POST /tasks/vul · 需 TASK_WRITE")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Partner-Id", value = "Partner ID", required = true,
                    paramType = "header", dataType = "string"),
            @ApiImplicitParam(name = "X-Request-Id", value = "请求追踪 ID", paramType = "header", dataType = "string"),
            @ApiImplicitParam(name = "Idempotency-Key", value = "写操作幂等键", paramType = "header", dataType = "string")
    })
    @PostMapping("/tasks/vul")
    public ApiResponse<CreateTaskResponse> createTaskByJson(@RequestBody CreateScanTaskByJsonRequest request) {
        return openTaskAppService.createTaskByJson(request);
    }

    /**
     * @deprecated 改用 {@link #createTaskByUpload(MultipartFile, String, Integer)}，
     * 以 multipart 文件上传提交 XML，免转义、对接更友好。本端点保留兼容存量调用方。
     */
    @Deprecated
    @ApiOperation(value = "创建扫描任务（XML 字符串）[已废弃]", notes = "§5.1.1 POST /tasks/file · 需 TASK_WRITE · 建议改用 POST /tasks/upload")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Partner-Id", value = "Partner ID", required = true,
                    paramType = "header", dataType = "string"),
            @ApiImplicitParam(name = "X-Request-Id", value = "请求追踪 ID", paramType = "header", dataType = "string"),
            @ApiImplicitParam(name = "Idempotency-Key", value = "写操作幂等键", paramType = "header", dataType = "string")
    })
    @PostMapping("/tasks/file")
    public ApiResponse<CreateTaskResponse> createTaskByFile(@RequestBody CreateScanTaskByFileRequest request) {
        return openTaskAppService.createTaskByFile(request);
    }

    @ApiOperation(value = "创建扫描任务（上传 XML 文件）", notes = "POST /tasks/upload · multipart/form-data · 需 TASK_WRITE")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Partner-Id", value = "Partner ID", required = true,
                    paramType = "header", dataType = "string"),
            @ApiImplicitParam(name = "X-Request-Id", value = "请求追踪 ID", paramType = "header", dataType = "string"),
            @ApiImplicitParam(name = "Idempotency-Key", value = "写操作幂等键", paramType = "header", dataType = "string")
    })
    @PostMapping(value = "/tasks/upload", consumes = "multipart/form-data")
    public ApiResponse<CreateTaskResponse> createTaskByUpload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("extTaskId") String extTaskId,
            @RequestParam("type") Integer type) {
        return openTaskAppService.createTaskByUpload(file, extTaskId, type);
    }

    @ApiOperation("查询任务进度")
    @GetMapping("/tasks/{taskId}")
    public ApiResponse<TaskProgressDto> getTask(@PathVariable("taskId") String taskId) {
        return openTaskAppService.getTask(taskId);
    }

    @ApiOperation("分页查询任务列表")
    @GetMapping("/tasks")
    public ApiResponse<TaskListPageDto> listTasks(
            @RequestParam(value = "extTaskId", required = false) String extTaskId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "createdFrom", required = false) String createdFrom,
            @RequestParam(value = "createdTo", required = false) String createdTo,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return openTaskAppService.listTasks(extTaskId, status, createdFrom, createdTo, page, size);
    }
}
