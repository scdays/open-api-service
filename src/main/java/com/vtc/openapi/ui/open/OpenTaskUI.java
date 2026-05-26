package com.vtc.openapi.ui.open;

import com.vtc.openapi.app.service.IOpenTaskAppService;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.open.task.CreateTaskRequest;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 开放平台任务 REST（/api/open/v1/tasks · P0）。
 */
@RestController
@RequestMapping(OpenApiConstants.API_PREFIX)
@Api(tags = "开放平台 · 任务")
public class OpenTaskUI {

    private final IOpenTaskAppService openTaskAppService;

    public OpenTaskUI(IOpenTaskAppService openTaskAppService) {
        this.openTaskAppService = openTaskAppService;
    }

    @ApiOperation(value = "创建扫描任务", notes = "需 partner-gateway 注入 X-Partner-Id；extTaskId 幂等")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Partner-Id", value = "Partner ID", required = true,
                    paramType = "header", dataType = "string"),
            @ApiImplicitParam(name = "X-Request-Id", value = "请求追踪 ID", paramType = "header", dataType = "string")
    })
    @PostMapping("/tasks")
    public ApiResponse<CreateTaskResponse> createTask(@RequestBody CreateTaskRequest request) {
        return openTaskAppService.createTask(request);
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
