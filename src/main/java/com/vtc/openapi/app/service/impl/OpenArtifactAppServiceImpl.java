package com.vtc.openapi.app.service.impl;

import com.vtc.openapi.app.convert.OpenArtifactAppConvertor;
import com.vtc.openapi.app.open.InvocationPipeline;
import com.vtc.openapi.app.service.IOpenArtifactAppService;
import com.vtc.openapi.domain.artifact.model.result.ArtifactDownloadResult;
import com.vtc.openapi.domain.artifact.model.result.ArtifactListResult;
import com.vtc.openapi.domain.artifact.model.result.ArtifactMetadataResult;
import com.vtc.openapi.domain.artifact.service.business.IOpenArtifactDomainService;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.open.OpenApiOperations;
import com.vtc.openapi.domain.partner.context.PartnerContext;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.open.artifact.ArtifactListPageDto;
import com.vtc.openapi.ui.dto.open.artifact.ArtifactMetadataDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class OpenArtifactAppServiceImpl implements IOpenArtifactAppService {

    private final InvocationPipeline invocationPipeline;
    private final IOpenArtifactDomainService openArtifactDomainService;
    private final OpenArtifactAppConvertor convertor;

    public OpenArtifactAppServiceImpl(InvocationPipeline invocationPipeline,
                                      IOpenArtifactDomainService openArtifactDomainService,
                                      OpenArtifactAppConvertor convertor) {
        this.invocationPipeline = invocationPipeline;
        this.openArtifactDomainService = openArtifactDomainService;
        this.convertor = convertor;
    }

    @Override
    public ApiResponse<ArtifactMetadataDto> getArtifact(String artifactId) {
        String partnerId = PartnerContext.requirePartnerId();
        return invocationPipeline.invoke(OpenApiOperations.GET_ARTIFACT, ctx -> {
            ArtifactMetadataResult result = openArtifactDomainService.get(ctx, partnerId, artifactId);
            return convertor.toDto(result);
        });
    }

    @Override
    public ResponseEntity<byte[]> downloadArtifact(String artifactId) {
        String partnerId = PartnerContext.requirePartnerId();
        ApiResponse<ArtifactDownloadResult> wrapped = invocationPipeline.invoke(
                OpenApiOperations.DOWNLOAD_ARTIFACT, ctx ->
                        openArtifactDomainService.download(ctx, partnerId, artifactId));
        if (wrapped.getCode() != OpenApiConstants.CODE_OK || wrapped.getData() == null) {
            throw new OpenApiException(wrapped.getCode(), wrapped.getMessage());
        }
        ArtifactDownloadResult data = wrapped.getData();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + data.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(data.getContentType()))
                .body(data.getContent());
    }

    @Override
    public ApiResponse<ArtifactListPageDto> listTaskArtifacts(String taskId, String exportStage,
                                                              String artifactSource, Integer page, Integer size) {
        String partnerId = PartnerContext.requirePartnerId();
        int p = page != null ? page : 1;
        int s = size != null ? size : 20;
        return invocationPipeline.invoke(OpenApiOperations.LIST_TASK_ARTIFACTS, ctx -> {
            ArtifactListResult result = openArtifactDomainService.listByTask(
                    ctx, partnerId, taskId, exportStage, artifactSource, p, s);
            return convertor.toPageDto(result);
        });
    }

    @Override
    public ApiResponse<ArtifactListPageDto> listExportArtifacts(String exportId, Integer page, Integer size) {
        String partnerId = PartnerContext.requirePartnerId();
        int p = page != null ? page : 1;
        int s = size != null ? size : 20;
        return invocationPipeline.invoke(OpenApiOperations.LIST_EXPORT_ARTIFACTS, ctx -> {
            ArtifactListResult result = openArtifactDomainService.listByExport(ctx, partnerId, exportId, p, s);
            return convertor.toPageDto(result);
        });
    }
}
