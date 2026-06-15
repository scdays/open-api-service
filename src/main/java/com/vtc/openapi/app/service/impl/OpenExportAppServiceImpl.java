package com.vtc.openapi.app.service.impl;

import com.vtc.openapi.app.convert.OpenExportAppConvertor;
import com.vtc.openapi.app.open.InvocationPipeline;
import com.vtc.openapi.app.service.IOpenExportAppService;
import com.vtc.openapi.domain.export.model.result.ExportDownloadResult;
import com.vtc.openapi.domain.export.model.result.ExportListResult;
import com.vtc.openapi.domain.export.model.result.ExportMetadataResult;
import com.vtc.openapi.domain.export.service.business.IOpenExportDomainService;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.open.OpenApiOperations;
import com.vtc.openapi.domain.partner.context.PartnerContext;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.open.export.ExportListPageDto;
import com.vtc.openapi.ui.dto.open.export.ExportMetadataDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class OpenExportAppServiceImpl implements IOpenExportAppService {

    private final InvocationPipeline invocationPipeline;
    private final IOpenExportDomainService openExportDomainService;
    private final OpenExportAppConvertor convertor;

    public OpenExportAppServiceImpl(InvocationPipeline invocationPipeline,
                                    IOpenExportDomainService openExportDomainService,
                                    OpenExportAppConvertor convertor) {
        this.invocationPipeline = invocationPipeline;
        this.openExportDomainService = openExportDomainService;
        this.convertor = convertor;
    }

    @Override
    public ApiResponse<ExportMetadataDto> getExport(String exportId) {
        String partnerId = PartnerContext.requirePartnerId();
        return invocationPipeline.invoke(OpenApiOperations.GET_EXPORT, ctx -> {
            ExportMetadataResult result = openExportDomainService.get(ctx, partnerId, exportId);
            return convertor.toDto(result);
        });
    }

    @Override
    public ResponseEntity<byte[]> downloadExport(String exportId) {
        String partnerId = PartnerContext.requirePartnerId();
        ApiResponse<ExportDownloadResult> wrapped = invocationPipeline.invoke(
                OpenApiOperations.DOWNLOAD_EXPORT, ctx -> {
                    ExportDownloadResult result = openExportDomainService.download(ctx, partnerId, exportId);
                    return result;
                });
        if (wrapped.getCode() != OpenApiConstants.CODE_OK || wrapped.getData() == null) {
            throw new OpenApiException(wrapped.getCode(), wrapped.getMessage());
        }
        ExportDownloadResult data = wrapped.getData();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + data.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(data.getContentType()))
                .body(data.getContent());
    }

    @Override
    public ApiResponse<ExportListPageDto> listTaskExports(String taskId, Integer page, Integer size) {
        String partnerId = PartnerContext.requirePartnerId();
        int p = page != null ? page : 1;
        int s = size != null ? size : 20;
        return invocationPipeline.invoke(OpenApiOperations.LIST_TASK_EXPORTS, ctx -> {
            ExportListResult result = openExportDomainService.listByTask(ctx, partnerId, taskId, p, s);
            return convertor.toPageDto(result);
        });
    }
}
