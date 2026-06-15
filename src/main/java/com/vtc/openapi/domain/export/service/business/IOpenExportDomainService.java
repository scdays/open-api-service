package com.vtc.openapi.domain.export.service.business;

import com.vtc.openapi.domain.export.model.result.ExportDownloadResult;
import com.vtc.openapi.domain.export.model.result.ExportListResult;
import com.vtc.openapi.domain.export.model.result.ExportMetadataResult;
import com.vtc.openapi.domain.open.model.InvocationContext;

public interface IOpenExportDomainService {

    ExportMetadataResult get(InvocationContext ctx, String partnerId, String exportId);

    ExportDownloadResult download(InvocationContext ctx, String partnerId, String exportId);

    ExportListResult listByTask(InvocationContext ctx, String partnerId, String taskId, int page, int size);
}
