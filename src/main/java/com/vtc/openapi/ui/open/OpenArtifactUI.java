package com.vtc.openapi.ui.open;

import com.vtc.openapi.app.service.IOpenArtifactAppService;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.open.artifact.ArtifactListPageDto;
import com.vtc.openapi.ui.dto.open.artifact.ArtifactMetadataDto;
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
@Api(tags = "开放平台 · 产物")
public class OpenArtifactUI {

    private final IOpenArtifactAppService openArtifactAppService;

    public OpenArtifactUI(IOpenArtifactAppService openArtifactAppService) {
        this.openArtifactAppService = openArtifactAppService;
    }

    @ApiOperation(value = "查询产物元数据", notes = "GET /artifacts/{artifactId} - ARTIFACT_READ")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Partner-Id", value = "Partner ID", required = true,
                    paramType = "header", dataType = "string")
    })
    @GetMapping("/artifacts/{artifactId}")
    public ApiResponse<ArtifactMetadataDto> getArtifact(@PathVariable("artifactId") String artifactId) {
        return openArtifactAppService.getArtifact(artifactId);
    }

    @ApiOperation(value = "下载产物文件", notes = "GET /artifacts/{artifactId}/download - ARTIFACT_READ")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Partner-Id", value = "Partner ID", required = true,
                    paramType = "header", dataType = "string")
    })
    @GetMapping("/artifacts/{artifactId}/download")
    public ResponseEntity<byte[]> downloadArtifact(@PathVariable("artifactId") String artifactId) {
        return openArtifactAppService.downloadArtifact(artifactId);
    }

    @ApiOperation(value = "任务产物列表", notes = "GET /tasks/{taskId}/artifacts - ARTIFACT_READ")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Partner-Id", value = "Partner ID", required = true,
                    paramType = "header", dataType = "string")
    })
    @GetMapping("/tasks/{taskId}/artifacts")
    public ApiResponse<ArtifactListPageDto> listTaskArtifacts(
            @PathVariable("taskId") String taskId,
            @RequestParam(value = "exportStage", required = false) String exportStage,
            @RequestParam(value = "artifactSource", required = false) String artifactSource,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        return openArtifactAppService.listTaskArtifacts(taskId, exportStage, artifactSource, page, size);
    }

    @ApiOperation(value = "外发关联产物列表", notes = "GET /exports/{exportId}/artifacts - ARTIFACT_READ")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Partner-Id", value = "Partner ID", required = true,
                    paramType = "header", dataType = "string")
    })
    @GetMapping("/exports/{exportId}/artifacts")
    public ApiResponse<ArtifactListPageDto> listExportArtifacts(
            @PathVariable("exportId") String exportId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        return openArtifactAppService.listExportArtifacts(exportId, page, size);
    }
}
