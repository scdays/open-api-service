package com.vtc.openapi.app.service.impl;

import com.vtc.openapi.app.service.IArtifactAdminAppService;
import com.vtc.openapi.domain.artifact.model.result.ArtifactDownloadResult;
import com.vtc.openapi.domain.artifact.service.business.IOpenArtifactDomainService;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.open.OpenApiOperations;
import com.vtc.openapi.domain.open.model.InvocationContext;
import com.vtc.openapi.domain.open.service.business.IInvocationDomainService;
import com.vtc.openapi.domain.partner.service.business.IPartnerDomainService;
import com.vtc.openapi.ui.dto.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class ArtifactAdminAppServiceImpl implements IArtifactAdminAppService {

    private final IPartnerDomainService partnerDomainService;
    private final IOpenArtifactDomainService openArtifactDomainService;
    private final IInvocationDomainService invocationDomainService;

    public ArtifactAdminAppServiceImpl(IPartnerDomainService partnerDomainService,
                                       IOpenArtifactDomainService openArtifactDomainService,
                                       IInvocationDomainService invocationDomainService) {
        this.partnerDomainService = partnerDomainService;
        this.openArtifactDomainService = openArtifactDomainService;
        this.invocationDomainService = invocationDomainService;
    }

    @Override
    public ResponseEntity<byte[]> downloadArtifact(String partnerId, String artifactId) {
        if (!StringUtils.hasText(partnerId) || !StringUtils.hasText(artifactId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "partnerId/artifactId 不能为空");
        }
        partnerDomainService.requireByPartnerId(partnerId.trim());
        String trimmedArtifactId = artifactId.trim();
        String requestId = "ADM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        InvocationContext ctx = new InvocationContext(
                partnerId.trim(),
                requestId,
                OpenApiOperations.DOWNLOAD_ARTIFACT,
                "GET",
                "/internal/admin/artifacts/" + trimmedArtifactId + "/download",
                null);
        invocationDomainService.start(ctx);
        ApiResponse<Void> auditResponse = ApiResponse.of(OpenApiConstants.CODE_ENGINE_FAILED, "服务内部错误", null);
        try {
            ArtifactDownloadResult result = openArtifactDomainService.download(ctx, partnerId.trim(), trimmedArtifactId);
            auditResponse = ApiResponse.ok(null);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + sanitizeFileName(result.getFileName()) + "\"")
                    .contentType(MediaType.parseMediaType(result.getContentType()))
                    .body(result.getContent());
        } catch (OpenApiException ex) {
            auditResponse = ApiResponse.of(ex.getCode(), ex.getMessage(), null);
            throw ex;
        } catch (RuntimeException ex) {
            auditResponse = ApiResponse.of(OpenApiConstants.CODE_ENGINE_FAILED, "服务内部错误", null);
            throw ex;
        } finally {
            auditResponse.setRequestId(requestId);
            invocationDomainService.finish(ctx, auditResponse);
        }
    }

    private String sanitizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "artifact.bin";
        }
        return fileName.replace("\"", "").replace("\r", "").replace("\n", "");
    }
}
