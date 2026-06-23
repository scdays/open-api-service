package com.vtc.openapi.domain.artifact.service.business.impl;

import com.botany.spore.core.page.PageInfo;
import com.vtc.openapi.domain.artifact.model.entity.OpenArtifactDO;
import com.vtc.openapi.domain.artifact.model.result.ArtifactDownloadResult;
import com.vtc.openapi.domain.artifact.model.result.ArtifactListResult;
import com.vtc.openapi.domain.artifact.model.result.ArtifactMetadataResult;
import com.vtc.openapi.domain.artifact.repository.IOpenArtifactRepository;
import com.vtc.openapi.domain.artifact.service.business.IOpenArtifactDomainService;
import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.export.repository.IOpenExportRepository;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.open.OpenApiOperations;
import com.vtc.openapi.domain.open.model.InvocationContext;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.infra.export.ExportFileStorageAdapter;
import com.vtc.openapi.ui.dto.admin.WebhookDeliveryLogDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class OpenArtifactDomainServiceImpl implements IOpenArtifactDomainService {

    private static final String STATUS_READY = "READY";

    private final IOpenArtifactRepository artifactRepository;
    private final IOpenExportRepository exportRepository;
    private final IOpenTaskRepository openTaskRepository;
    private final ExportFileStorageAdapter fileStorage;

    public OpenArtifactDomainServiceImpl(IOpenArtifactRepository artifactRepository,
                                         IOpenExportRepository exportRepository,
                                         IOpenTaskRepository openTaskRepository,
                                         ExportFileStorageAdapter fileStorage) {
        this.artifactRepository = artifactRepository;
        this.exportRepository = exportRepository;
        this.openTaskRepository = openTaskRepository;
        this.fileStorage = fileStorage;
    }

    @Override
    public ArtifactMetadataResult get(InvocationContext ctx, String partnerId, String artifactId) {
        OpenArtifactDO row = requireOwnedArtifact(partnerId, artifactId);
        ctx.setResourceType(OpenApiOperations.RESOURCE_TYPE_ARTIFACT);
        ctx.setResourceId(artifactId);
        return toMetadata(row);
    }

    @Override
    public ArtifactDownloadResult download(InvocationContext ctx, String partnerId, String artifactId) {
        OpenArtifactDO row = requireOwnedArtifact(partnerId, artifactId);
        if (!STATUS_READY.equals(row.getStatus())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "产物文件未就绪");
        }
        if (row.getExpiresAt() != null && row.getExpiresAt().before(new Date())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "产物文件已过期");
        }
        if (!StringUtils.hasText(row.getFileField())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "产物文件不存在");
        }
        byte[] bytes = fileStorage.read(row.getFileField());
        ArtifactDownloadResult result = new ArtifactDownloadResult();
        result.setContent(bytes);
        result.setContentType(StringUtils.hasText(row.getContentType())
                ? row.getContentType() : "application/octet-stream");
        result.setFileName(StringUtils.hasText(row.getFileName())
                ? row.getFileName() : "artifact-" + artifactId);
        ctx.setResourceType(OpenApiOperations.RESOURCE_TYPE_ARTIFACT);
        ctx.setResourceId(artifactId);
        return result;
    }

    @Override
    public ArtifactListResult listByTask(InvocationContext ctx, String partnerId, String taskId,
                                         String exportStage, String artifactSource, int page, int size) {
        requireOwnedTask(partnerId, taskId);
        validatePage(page, size);
        PageInfo<OpenArtifactDO> pageResult = artifactRepository.pageByTask(
                partnerId, taskId, clean(exportStage), clean(artifactSource), page, size);
        ctx.setResourceType(OpenApiOperations.RESOURCE_TYPE_TASK);
        ctx.setResourceId(taskId);
        return toListResult(pageResult, page, size);
    }

    @Override
    public ArtifactListResult listByExport(InvocationContext ctx, String partnerId, String exportId,
                                           int page, int size) {
        validatePage(page, size);
        OpenExportDO export = exportRepository.findByPartnerAndExportId(partnerId, exportId);
        if (export == null) {
            throw new OpenApiException(OpenApiConstants.CODE_CROSS_PARTNER, "外发记录不存在或无权访问");
        }
        PageInfo<OpenArtifactDO> pageResult = artifactRepository.pageByTaskAndStage(
                partnerId, export.getTaskId(), export.getExportStage(), page, size);
        ctx.setResourceType(OpenApiOperations.RESOURCE_TYPE_EXPORT);
        ctx.setResourceId(exportId);
        return toListResult(pageResult, page, size);
    }

    @Override
    public void enrichWebhookDelivery(WebhookDeliveryLogDTO dto) {
        if (dto == null) {
            return;
        }
        if (!StringUtils.hasText(dto.getArtifactId())) {
            dto.setArtifactDownloadable(Boolean.FALSE);
            return;
        }
        dto.setArtifactDownloadable(isArtifactRowDownloadable(dto.getPartnerId(), dto.getArtifactId().trim()));
    }

    private boolean isArtifactRowDownloadable(String partnerId, String artifactId) {
        if (!StringUtils.hasText(artifactId)) {
            return false;
        }
        OpenArtifactDO row = artifactRepository.findByPartnerAndArtifactId(partnerId, artifactId);
        if (row == null || !STATUS_READY.equals(row.getStatus())) {
            return false;
        }
        if (row.getExpiresAt() != null && row.getExpiresAt().before(new Date())) {
            return false;
        }
        return StringUtils.hasText(row.getFileField());
    }

    private OpenArtifactDO requireOwnedArtifact(String partnerId, String artifactId) {
        if (!StringUtils.hasText(artifactId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "artifactId 不能为空");
        }
        OpenArtifactDO row = artifactRepository.findByPartnerAndArtifactId(partnerId, artifactId);
        if (row == null) {
            throw new OpenApiException(OpenApiConstants.CODE_CROSS_PARTNER, "产物记录不存在或无权访问");
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

    private static void validatePage(int page, int size) {
        if (page < 1) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "page 必须从 1 开始");
        }
        if (size < 1 || size > 1000) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "size 必须在 1-1000 之间");
        }
    }

    private static ArtifactListResult toListResult(PageInfo<OpenArtifactDO> pageResult, int page, int size) {
        ArtifactListResult data = new ArtifactListResult();
        data.setPage(page);
        data.setSize(size);
        data.setTotal(pageResult.getTotal());
        if (!CollectionUtils.isEmpty(pageResult.getRecords())) {
            data.setItems(pageResult.getRecords().stream()
                    .map(OpenArtifactDomainServiceImpl::toMetadata)
                    .collect(Collectors.toList()));
        }
        return data;
    }

    private static ArtifactMetadataResult toMetadata(OpenArtifactDO row) {
        ArtifactMetadataResult dto = new ArtifactMetadataResult();
        dto.setArtifactId(row.getArtifactId());
        dto.setTaskId(row.getTaskId());
        dto.setExtTaskId(row.getExtTaskId());
        dto.setExportId(row.getExportId());
        dto.setExportStage(row.getExportStage());
        dto.setArtifactSource(row.getArtifactSource());
        dto.setReportTypeCode(row.getReportTypeCode());
        dto.setReportTypeName(row.getReportTypeName());
        dto.setScannerVendor(row.getScannerVendor());
        dto.setScannerProduct(row.getScannerProduct());
        dto.setSubTaskId(row.getSubTaskId());
        dto.setFileName(row.getFileName());
        dto.setFileFormat(row.getFileFormat());
        dto.setContentType(row.getContentType());
        dto.setByteSize(row.getByteSize());
        dto.setChecksum(row.getChecksum());
        dto.setStatus(row.getStatus());
        dto.setGeneratedAt(row.getGeneratedAt() != null ? row.getGeneratedAt() : row.getCreatedAt());
        dto.setExpiresAt(row.getExpiresAt());
        dto.setDownloadUrl(row.getDownloadUrl());
        dto.setErrorMessage(row.getErrorMessage());
        return dto;
    }

    private static String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
