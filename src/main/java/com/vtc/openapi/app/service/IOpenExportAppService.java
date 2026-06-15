package com.vtc.openapi.app.service;

import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.open.export.ExportListPageDto;
import com.vtc.openapi.ui.dto.open.export.ExportMetadataDto;
import org.springframework.http.ResponseEntity;

public interface IOpenExportAppService {

    ApiResponse<ExportMetadataDto> getExport(String exportId);

    ResponseEntity<byte[]> downloadExport(String exportId);

    ApiResponse<ExportListPageDto> listTaskExports(String taskId, Integer page, Integer size);
}
