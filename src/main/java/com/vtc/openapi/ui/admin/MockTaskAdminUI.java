package com.vtc.openapi.ui.admin;

import com.vtc.openapi.app.service.IMockTaskAdminAppService;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.MockBundleStatusDto;
import com.vtc.openapi.ui.dto.admin.MockDispatchPacketDto;
import com.vtc.openapi.ui.dto.admin.MockImportPreviewResultDto;
import com.vtc.openapi.ui.dto.admin.MockImportReportResultDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Mock manual ingest admin API (internal).
 */
@RestController
@RequestMapping("/internal/admin/mock-tasks")
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "mock")
@Api(tags = "Mock manual ingest")
public class MockTaskAdminUI {

    private final IMockTaskAdminAppService mockTaskAdminAppService;

    public MockTaskAdminUI(IMockTaskAdminAppService mockTaskAdminAppService) {
        this.mockTaskAdminAppService = mockTaskAdminAppService;
    }

    @ApiOperation("Import NSFocus XML report and trigger FINISHED + instance ingest")
    @PostMapping("/{taskId}/import-report")
    public ApiResponse<MockImportReportResultDto> importReport(
            @PathVariable("taskId") String taskId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "force", defaultValue = "false") boolean force) {
        return mockTaskAdminAppService.importReport(taskId, file, force);
    }

    @ApiOperation("Preview XML parse result without persisting")
    @PostMapping("/{taskId}/preview-report")
    public ApiResponse<MockImportPreviewResultDto> previewReport(
            @PathVariable("taskId") String taskId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "sampleSize", defaultValue = "10") int sampleSize) {
        return mockTaskAdminAppService.previewReport(taskId, file, sampleSize);
    }

    @ApiOperation("Task bundle and ingest status")
    @GetMapping("/{taskId}/bundle-status")
    public ApiResponse<MockBundleStatusDto> bundleStatus(@PathVariable("taskId") String taskId) {
        return mockTaskAdminAppService.getBundleStatus(taskId);
    }

    @ApiOperation("Dispatch context for manual scanner workflow")
    @GetMapping("/{taskId}/dispatch-packet")
    public ApiResponse<MockDispatchPacketDto> dispatchPacket(@PathVariable("taskId") String taskId) {
        return mockTaskAdminAppService.getDispatchPacket(taskId);
    }
}
