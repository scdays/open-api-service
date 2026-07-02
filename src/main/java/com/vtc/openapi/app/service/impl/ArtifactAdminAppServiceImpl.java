package com.vtc.openapi.app.service.impl;

import com.vtc.openapi.app.service.IArtifactAdminAppService;
import com.vtc.openapi.domain.artifact.model.entity.OpenArtifactDO;
import com.vtc.openapi.domain.artifact.model.result.ArtifactDownloadResult;
import com.vtc.openapi.domain.artifact.repository.IOpenArtifactRepository;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ArtifactAdminAppServiceImpl implements IArtifactAdminAppService {

    private final IPartnerDomainService partnerDomainService;
    private final IOpenArtifactDomainService openArtifactDomainService;
    private final IInvocationDomainService invocationDomainService;
    private final IOpenArtifactRepository artifactRepository;

    public ArtifactAdminAppServiceImpl(IPartnerDomainService partnerDomainService,
                                       IOpenArtifactDomainService openArtifactDomainService,
                                       IInvocationDomainService invocationDomainService,
                                       IOpenArtifactRepository artifactRepository) {
        this.partnerDomainService = partnerDomainService;
        this.openArtifactDomainService = openArtifactDomainService;
        this.invocationDomainService = invocationDomainService;
        this.artifactRepository = artifactRepository;
    }

    @Override
    public ResponseEntity<byte[]> downloadArtifact(String partnerId, String artifactId) {
        if (!StringUtils.hasText(partnerId) || !StringUtils.hasText(artifactId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "partnerId/artifactId 不能为空");
        }
        String trimmedArtifactId = artifactId.trim();
        String requestId = "ADM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        InvocationContext ctx = new InvocationContext(
                partnerId.trim(),
                requestId,
                OpenApiOperations.DOWNLOAD_ARTIFACT,
                "GET",
                "/internal/admin/artifacts/" + trimmedArtifactId + "/download",
                null);
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

    @Override
    public ResponseEntity<byte[]> downloadArtifactByEventId(String partnerId, String eventId) {
        if (!StringUtils.hasText(partnerId) || !StringUtils.hasText(eventId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "partnerId/eventId 不能为空");
        }
        List<OpenArtifactDO> artifacts = artifactRepository.listByWebhookEventIds(Collections.singleton(eventId.trim()));
        if (artifacts == null || artifacts.isEmpty()) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "未找到 eventId 对应的产物记录");
        }
        OpenArtifactDO artifact = artifacts.get(0);
        return downloadArtifact(partnerId, artifact.getArtifactId());
    }
}
