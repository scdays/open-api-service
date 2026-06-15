package com.vtc.openapi.app.service;

import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.MockBundleStatusDto;
import com.vtc.openapi.ui.dto.admin.MockDispatchPacketDto;
import com.vtc.openapi.ui.dto.admin.MockImportPreviewResultDto;
import com.vtc.openapi.ui.dto.admin.MockImportReportResultDto;
import org.springframework.web.multipart.MultipartFile;

/**
 * Mock manual ingest admin operations.
 */
public interface IMockTaskAdminAppService {

    ApiResponse<MockImportReportResultDto> importReport(String taskId, MultipartFile file, boolean force);

    ApiResponse<MockImportPreviewResultDto> previewReport(String taskId, MultipartFile file, int sampleSize);

    ApiResponse<MockBundleStatusDto> getBundleStatus(String taskId);

    ApiResponse<MockDispatchPacketDto> getDispatchPacket(String taskId);
}
