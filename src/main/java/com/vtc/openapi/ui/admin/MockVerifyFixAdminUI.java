package com.vtc.openapi.ui.admin;

import com.vtc.openapi.app.service.IMockVerifyFixAdminAppService;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.MockVerifyFixCompleteResultDto;
import com.vtc.openapi.ui.dto.admin.MockVerifyFixJobDto;
import com.vtc.openapi.ui.dto.admin.OfflineTaskVerifyFixContextDto;
import com.vtc.openapi.ui.dto.admin.VerifyFixInvocationCandidateDto;
import com.vtc.openapi.ui.params.admin.CreateInternalVerifyFixJobParams;
import com.vtc.openapi.ui.params.admin.CreateVerifyFixJobFromSelectionParams;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/internal/admin/mock-verify-fix")
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "mock")
@Api(tags = "Mock verify-fix ops")
@Validated
public class MockVerifyFixAdminUI {

    private final IMockVerifyFixAdminAppService appService;

    public MockVerifyFixAdminUI(IMockVerifyFixAdminAppService appService) {
        this.appService = appService;
    }

    @ApiOperation("待处理/历史修复核验任务列表")
    @GetMapping("/jobs")
    public ApiResponse<List<MockVerifyFixJobDto>> listJobs(
            @RequestParam(value = "partnerId", required = false) String partnerId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return appService.listJobs(partnerId, status, limit);
    }

    @ApiOperation("修复核验任务详情")
    @GetMapping("/jobs/{jobId}")
    public ApiResponse<MockVerifyFixJobDto> getJob(@PathVariable("jobId") String jobId) {
        return appService.getJob(jobId);
    }

    @ApiOperation("导入复扫 XML 并自动比对完成（仅 Webhook，不外发）")
    @PostMapping("/jobs/{jobId}/import-rescan-xml")
    public ApiResponse<MockVerifyFixCompleteResultDto> importRescanXml(
            @PathVariable("jobId") String jobId,
            @RequestPart("file") MultipartFile file) {
        return appService.importRescanXml(jobId, file);
    }

    @ApiOperation("一键核验修复：全部目标 → 6")
    @PostMapping("/jobs/{jobId}/complete-all-fixed")
    public ApiResponse<MockVerifyFixCompleteResultDto> completeAllFixed(@PathVariable("jobId") String jobId) {
        return appService.completeAllFixed(jobId);
    }

    @ApiOperation("一键核验未修复：全部目标 → 7")
    @PostMapping("/jobs/{jobId}/complete-all-unfixed")
    public ApiResponse<MockVerifyFixCompleteResultDto> completeAllUnfixed(@PathVariable("jobId") String jobId) {
        return appService.completeAllUnfixed(jobId);
    }

    @ApiOperation("按已导入/任务 bundle 报告比对完成")
    @PostMapping("/jobs/{jobId}/complete-by-compare")
    public ApiResponse<MockVerifyFixCompleteResultDto> completeByCompare(@PathVariable("jobId") String jobId) {
        return appService.completeByCompare(jobId);
    }

    @ApiOperation("离线导入任务上下文（实例状态分布 / 可核验列表）")
    @GetMapping("/offline-tasks/{taskId}/context")
    public ApiResponse<OfflineTaskVerifyFixContextDto> offlineTaskContext(
            @PathVariable("taskId") String taskId,
            @RequestParam("partnerId") String partnerId) {
        return appService.getOfflineTaskContext(partnerId, taskId);
    }

    @ApiOperation("从离线导入任务创建平台内部修复核验任务（不经 Partner API 建扫任务）")
    @PostMapping("/jobs/create-from-offline-task")
    public ApiResponse<MockVerifyFixJobDto> createFromOfflineTask(
            @Valid @RequestBody CreateInternalVerifyFixJobParams params) {
        return appService.createFromOfflineTask(params);
    }

    @ApiOperation("Partner 修复核验调用记录中的待处理实例")
    @GetMapping("/invocation-candidates")
    public ApiResponse<List<VerifyFixInvocationCandidateDto>> invocationCandidates(
            @RequestParam("partnerId") String partnerId,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {
        return appService.listInvocationCandidates(partnerId, limit);
    }

    @ApiOperation("从所选 vulInfoID 归入已有 PENDING 作业或新建合并作业")
    @PostMapping("/jobs/create-from-selection")
    public ApiResponse<MockVerifyFixJobDto> createFromSelection(
            @Valid @RequestBody CreateVerifyFixJobFromSelectionParams params) {
        return appService.createFromSelection(params);
    }
}
