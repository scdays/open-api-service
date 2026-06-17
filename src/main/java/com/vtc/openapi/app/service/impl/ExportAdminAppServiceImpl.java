package com.vtc.openapi.app.service.impl;

import com.vtc.openapi.app.service.IExportAdminAppService;
import com.vtc.openapi.domain.export.model.result.ExportDownloadResult;
import com.vtc.openapi.domain.export.service.business.IOpenExportDomainService;
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
public class ExportAdminAppServiceImpl implements IExportAdminAppService {

    private final IPartnerDomainService partnerDomainService;
    private final IOpenExportDomainService openExportDomainService;
    private final IInvocationDomainService invocationDomainService;

    public ExportAdminAppServiceImpl(IPartnerDomainService partnerDomainService,
                                     IOpenExportDomainService openExportDomainService,
                                     IInvocationDomainService invocationDomainService) {
        this.partnerDomainService = partnerDomainService;
        this.openExportDomainService = openExportDomainService;
        this.invocationDomainService = invocationDomainService;
    }

    @Override
    public ResponseEntity<byte[]> downloadExport(String partnerId, String exportId) {
        if (!StringUtils.hasText(partnerId) || !StringUtils.hasText(exportId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "partnerId/exportId 不能为空");
        }
        partnerDomainService.requireByPartnerId(partnerId.trim());
        String trimmedExportId = exportId.trim();
        String requestId = "ADM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        InvocationContext ctx = new InvocationContext(
                partnerId.trim(),
                requestId,
                OpenApiOperations.DOWNLOAD_EXPORT,
                "GET",
                "/internal/admin/exports/" + trimmedExportId + "/download",
                null);
        invocationDomainService.start(ctx);
        ApiResponse<Void> auditResponse = ApiResponse.of(OpenApiConstants.CODE_ENGINE_FAILED, "服务内部错误", null);
        try {
            ExportDownloadResult result = openExportDomainService.download(ctx, partnerId.trim(), trimmedExportId);
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
            return "export.bin";
        }
        return fileName.replace("\"", "").replace("\r", "").replace("\n", "");
    }
}
