package com.vtc.openapi.ui.admin;

import com.botany.spore.ddd.ui.BaseUI;
import com.vtc.openapi.app.service.IOpenTaskAdminAppService;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.OpenTaskDispatchRetryResultDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskAdminPageDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskSurveyRefetchResultDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskSurveyResultsDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskWorkspaceDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/admin/open-tasks")
@Validated
@Api(tags = "OPEN 编排任务运营")
public class OpenTaskAdminUI extends BaseUI {

    private final IOpenTaskAdminAppService openTaskAdminAppService;

    public OpenTaskAdminUI(IOpenTaskAdminAppService openTaskAdminAppService) {
        this.openTaskAdminAppService = openTaskAdminAppService;
    }

    @ApiOperation("分页查询 OPEN 编排任务")
    @GetMapping
    public ApiResponse<OpenTaskAdminPageDto> listTasks(
            @RequestParam(value = "partnerId", required = false) String partnerId,
            @RequestParam(value = "taskId", required = false) String taskId,
            @RequestParam(value = "extTaskId", required = false) String extTaskId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "scanTemplateId", required = false) Integer scanTemplateId,
            @RequestParam(value = "vulnType", required = false) Integer vulnType,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return openTaskAdminAppService.listTasks(
                partnerId, taskId, extTaskId, status, scanTemplateId, vulnType, page, size);
    }

    @ApiOperation("任务实例工作台详情")
    @GetMapping("/{taskId}/workspace")
    public ApiResponse<OpenTaskWorkspaceDto> getWorkspace(@PathVariable("taskId") String taskId) {
        return openTaskAdminAppService.getWorkspace(taskId);
    }

    @ApiOperation("排查/验证阶段 VTC 扫描结果（存活/端口/漏洞）")
    @GetMapping("/{taskId}/survey-results")
    public ApiResponse<OpenTaskSurveyResultsDto> getSurveyResults(
            @PathVariable("taskId") String taskId,
            @RequestParam(value = "scanPhase", defaultValue = "1") Integer scanPhase,
            @RequestParam(value = "subId", required = false) String subId) {
        return openTaskAdminAppService.getSurveyResults(taskId, scanPhase, subId);
    }

    @ApiOperation("重新从 VTC 获取排查子任务扫描结果（清除旧数据后落库并 replay 漏洞生命周期）")
    @PostMapping("/{taskId}/survey-refetch")
    public ApiResponse<OpenTaskSurveyRefetchResultDto> refetchSurveyResults(
            @PathVariable("taskId") String taskId,
            @RequestParam("subId") String subId) {
        return openTaskAdminAppService.refetchSurveyResults(taskId, subId);
    }

    @ApiOperation("手动重试子任务 VTC 下发（排查/验证阶段 FAILED 子任务）")
    @PostMapping("/{taskId}/retry-dispatch")
    public ApiResponse<OpenTaskDispatchRetryResultDto> retryDispatch(
            @PathVariable("taskId") String taskId,
            @RequestParam(value = "scanPhase", defaultValue = "1") Integer scanPhase,
            @RequestParam(value = "subId", required = false) String subId) {
        return openTaskAdminAppService.retrySurveyDispatch(taskId, scanPhase, subId);
    }
}
