package com.vtc.openapi.app.service;

import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.open.artifact.ArtifactListPageDto;
import com.vtc.openapi.ui.dto.open.artifact.ArtifactMetadataDto;
import org.springframework.http.ResponseEntity;

public interface IOpenArtifactAppService {

    ApiResponse<ArtifactMetadataDto> getArtifact(String artifactId);

    ResponseEntity<byte[]> downloadArtifact(String artifactId);

    ApiResponse<ArtifactListPageDto> listTaskArtifacts(String taskId, String exportStage,
                                                       String artifactSource, Integer page, Integer size);

    ApiResponse<ArtifactListPageDto> listExportArtifacts(String exportId, Integer page, Integer size);
}
