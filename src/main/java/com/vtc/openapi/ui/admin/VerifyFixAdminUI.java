package com.vtc.openapi.ui.admin;

import com.vtc.openapi.app.service.IVerifyFixAdminAppService;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.MockVerifyFixJobDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskSurveyRefetchResultDto;
import com.vtc.openapi.ui.dto.admin.VerifyFixPendingInstanceDto;
import com.vtc.openapi.ui.dto.admin.VerifyFixWorkspaceDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/admin/verify-fix")
@Validated
@Api(tags = "修复核验运营")
public class VerifyFixAdminUI {

    private final IVerifyFixAdminAppService appService;

    public VerifyFixAdminUI(IVerifyFixAdminAppService appService) {
        this.appService = appService;
    }

    @ApiOperation("修复核验任务列表")
    @GetMapping("/jobs")
    public ApiResponse<List<MockVerifyFixJobDto>> listJobs(
            @RequestParam(value = "partnerId", required = false) String partnerId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "taskId", required = false) String taskId,
            @RequestParam(value = "jobId", required = false) String jobId,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return appService.listJobs(partnerId, status, taskId, jobId, limit);
    }

    @ApiOperation("修复核验工作台")
    @GetMapping("/jobs/{jobId}/workspace")
    public ApiResponse<VerifyFixWorkspaceDto> getWorkspace(@PathVariable("jobId") String jobId) {
        return appService.getWorkspace(jobId);
    }

    @ApiOperation("重新获取复扫子任务扫描结果（phase=3）")
    @PostMapping("/jobs/{jobId}/rescan-refetch")
    public ApiResponse<OpenTaskSurveyRefetchResultDto> refetchRescanSub(
            @PathVariable("jobId") String jobId,
            @RequestParam("subId") String subId) {
        return appService.refetchRescanSub(jobId, subId);
    }

    @ApiOperation("手动重试下发复扫（不受自动重试 5 次上限约束）")
    @PostMapping("/jobs/{jobId}/retry-dispatch")
    public ApiResponse<Boolean> retryDispatch(@PathVariable("jobId") String jobId) {
        return appService.retryDispatch(jobId);
    }

    @ApiOperation("手动重试单个复扫子任务（仅重新下发指定 sub）")
    @PostMapping("/jobs/{jobId}/subs/{subId}/retry-dispatch")
    public ApiResponse<Boolean> retryDispatchSub(@PathVariable("jobId") String jobId,
                                                 @PathVariable("subId") String subId) {
        return appService.retryDispatchSub(jobId, subId);
    }

    @ApiOperation("待修复核验系统漏洞列表（默认按 taskId 筛选）")
    @GetMapping("/pending-instances")
    public ApiResponse<List<VerifyFixPendingInstanceDto>> listPendingInstances(
            @RequestParam("partnerId") String partnerId,
            @RequestParam(value = "taskId", required = false) String taskId,
            @RequestParam(value = "jobId", required = false) String jobId,
            @RequestParam(value = "limit", defaultValue = "200") int limit) {
        return appService.listPendingInstances(partnerId, taskId, jobId, limit);
    }
}
