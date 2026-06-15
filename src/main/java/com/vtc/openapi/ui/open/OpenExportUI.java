package com.vtc.openapi.ui.open;

import com.vtc.openapi.app.service.IOpenExportAppService;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.open.export.ExportListPageDto;
import com.vtc.openapi.ui.dto.open.export.ExportMetadataDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(OpenApiConstants.API_PREFIX)
@Api(tags = "开放平台 · 外发")
public class OpenExportUI {

    private final IOpenExportAppService openExportAppService;

    public OpenExportUI(IOpenExportAppService openExportAppService) {
        this.openExportAppService = openExportAppService;
    }

    @ApiOperation(value = "查询外发元数据", notes = "GET /exports/{exportId} - EXPORT_READ")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Partner-Id", value = "Partner ID", required = true,
                    paramType = "header", dataType = "string")
    })
    @GetMapping("/exports/{exportId}")
    public ApiResponse<ExportMetadataDto> getExport(@PathVariable("exportId") String exportId) {
        return openExportAppService.getExport(exportId);
    }

    @ApiOperation(value = "下载外发文件", notes = "GET /exports/{exportId}/download - EXPORT_READ")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Partner-Id", value = "Partner ID", required = true,
                    paramType = "header", dataType = "string")
    })
    @GetMapping("/exports/{exportId}/download")
    public ResponseEntity<byte[]> downloadExport(@PathVariable("exportId") String exportId) {
        return openExportAppService.downloadExport(exportId);
    }

    @ApiOperation(value = "任务外发列表", notes = "GET /tasks/{taskId}/exports - EXPORT_READ")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Partner-Id", value = "Partner ID", required = true,
                    paramType = "header", dataType = "string")
    })
    @GetMapping("/tasks/{taskId}/exports")
    public ApiResponse<ExportListPageDto> listTaskExports(
            @PathVariable("taskId") String taskId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        return openExportAppService.listTaskExports(taskId, page, size);
    }
}
