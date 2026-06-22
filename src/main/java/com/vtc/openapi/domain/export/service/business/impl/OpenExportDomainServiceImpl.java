package com.vtc.openapi.domain.export.service.business.impl;

import com.botany.spore.core.page.PageInfo;
import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.export.model.entity.OpenExportFileDO;
import com.vtc.openapi.domain.export.model.result.ExportDownloadResult;
import com.vtc.openapi.domain.export.model.result.ExportListResult;
import com.vtc.openapi.domain.export.model.result.ExportMetadataResult;
import com.vtc.openapi.domain.export.repository.IOpenExportRepository;
import com.vtc.openapi.domain.export.service.business.IExportDownloadPolicy;
import com.vtc.openapi.domain.export.service.business.IOpenExportDomainService;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.open.OpenApiOperations;
import com.vtc.openapi.domain.open.model.InvocationContext;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.infra.export.ExportFileStorageAdapter;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class OpenExportDomainServiceImpl implements IOpenExportDomainService {

    private static final String STATUS_READY = "READY";

    private final IOpenExportRepository exportRepository;
    private final IOpenTaskRepository openTaskRepository;
    private final ExportFileStorageAdapter fileStorage;
    private final IExportDownloadPolicy downloadPolicy;

    public OpenExportDomainServiceImpl(IOpenExportRepository exportRepository,
                                       IOpenTaskRepository openTaskRepository,
                                       ExportFileStorageAdapter fileStorage,
                                       IExportDownloadPolicy downloadPolicy) {
        this.exportRepository = exportRepository;
        this.openTaskRepository = openTaskRepository;
        this.fileStorage = fileStorage;
        this.downloadPolicy = downloadPolicy;
    }

    @Override
    public ExportMetadataResult get(InvocationContext ctx, String partnerId, String exportId) {
        OpenExportDO row = requireOwnedExport(partnerId, exportId);
        ctx.setResourceType(OpenApiOperations.RESOURCE_TYPE_EXPORT);
        ctx.setResourceId(exportId);
        return toMetadata(row);
    }

    @Override
    public ExportDownloadResult download(InvocationContext ctx, String partnerId, String exportId) {
        OpenExportDO row = requireOwnedExport(partnerId, exportId);
        if (!STATUS_READY.equals(row.getStatus())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "外发文件未就绪");
        }
        if (row.getExpiresAt() != null && row.getExpiresAt().before(new Date())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "外发文件已过期");
        }
        if (!downloadPolicy.isStageDownloadable(partnerId, row.getExportStage())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "该外发类型不支持下载");
        }
        OpenExportFileDO file = exportRepository.findFileByExportId(exportId);
        if (file == null || !StringUtils.hasText(file.getFileField())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "外发文件不存在");
        }
        byte[] bytes = fileStorage.read(file.getFileField());
        ExportDownloadResult result = new ExportDownloadResult();
        result.setContent(bytes);
        String format = row.getFormat();
        result.setContentType(resolveContentType(format));
        String fallbackExt = StringUtils.hasText(format) ? format.toLowerCase() : "xml";
        result.setFileName(file.getFileMetadata() != null ? file.getFileMetadata()
                : "export-" + exportId + "." + fallbackExt);
        ctx.setResourceType(OpenApiOperations.RESOURCE_TYPE_EXPORT);
        ctx.setResourceId(exportId);
        return result;
    }

    private static String resolveContentType(String format) {
        if (format == null) {
            return "application/octet-stream";
        }
        switch (format.toLowerCase()) {
            case "json":
                return "application/json";
            case "xml":
                return "application/xml";
            case "zip":
                return "application/zip";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "pdf":
                return "application/pdf";
            default:
                return "application/octet-stream";
        }
    }

    @Override
    public ExportListResult listByTask(InvocationContext ctx, String partnerId, String taskId, int page, int size) {
        requireOwnedTask(partnerId, taskId);
        if (page < 1) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "page 必须从 1 开始");
        }
        if (size < 1 || size > 1000) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "size 必须在 1-1000 之间");
        }
        PageInfo<OpenExportDO> pageResult = exportRepository.pageByTask(partnerId, taskId, page, size);
        ExportListResult data = new ExportListResult();
        data.setPage(page);
        data.setSize(size);
        data.setTotal(pageResult.getTotal());
        if (!CollectionUtils.isEmpty(pageResult.getRecords())) {
            data.setItems(pageResult.getRecords().stream()
                    .map(this::toMetadata)
                    .collect(Collectors.toList()));
        }
        ctx.setResourceType(OpenApiOperations.RESOURCE_TYPE_TASK);
        ctx.setResourceId(taskId);
        return data;
    }

    private OpenExportDO requireOwnedExport(String partnerId, String exportId) {
        if (!StringUtils.hasText(exportId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "exportId 不能为空");
        }
        OpenExportDO row = exportRepository.findByPartnerAndExportId(partnerId, exportId);
        if (row == null) {
            throw new OpenApiException(OpenApiConstants.CODE_CROSS_PARTNER, "外发记录不存在或无权访问");
        }
        return row;
    }

    private void requireOwnedTask(String partnerId, String taskId) {
        OpenTaskDO task = openTaskRepository.findByTaskId(taskId);
        if (task == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "任务不存在");
        }
        if (!Objects.equals(partnerId, task.getPartnerId())) {
            throw new OpenApiException(OpenApiConstants.CODE_CROSS_PARTNER, "无权访问该任务");
        }
    }

    private ExportMetadataResult toMetadata(OpenExportDO row) {
        ExportMetadataResult dto = new ExportMetadataResult();
        dto.setExportId(row.getExportId());
        dto.setTaskId(row.getTaskId());
        dto.setExtTaskId(row.getExtTaskId());
        dto.setReportTemplateId(row.getReportTemplateId());
        dto.setFormat(row.getFormat());
        dto.setExportStage(row.getExportStage());
        dto.setDataType(row.getDataType());
        dto.setStatus(row.getStatus());
        dto.setRecordCount(row.getRecordCount());
        dto.setExpiresAt(row.getExpiresAt());
        dto.setCreatedAt(row.getCreatedAt() != null ? row.getCreatedAt() : row.getGeneratedAt());
        dto.setDownloadUrl(row.getDownloadUrl());
        return dto;
    }
}
