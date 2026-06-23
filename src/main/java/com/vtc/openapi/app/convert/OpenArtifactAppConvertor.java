package com.vtc.openapi.app.convert;

import com.vtc.openapi.domain.artifact.model.result.ArtifactListResult;
import com.vtc.openapi.domain.artifact.model.result.ArtifactMetadataResult;
import com.vtc.openapi.ui.dto.open.artifact.ArtifactListPageDto;
import com.vtc.openapi.ui.dto.open.artifact.ArtifactMetadataDto;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OpenArtifactAppConvertor {

    private static final DateTimeFormatter ISO_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    public ArtifactMetadataDto toDto(ArtifactMetadataResult result) {
        if (result == null) {
            return null;
        }
        ArtifactMetadataDto dto = new ArtifactMetadataDto();
        dto.setArtifactId(result.getArtifactId());
        dto.setTaskId(result.getTaskId());
        dto.setExtTaskId(result.getExtTaskId());
        dto.setExportId(result.getExportId());
        dto.setExportStage(result.getExportStage());
        dto.setArtifactSource(result.getArtifactSource());
        dto.setReportTypeCode(result.getReportTypeCode());
        dto.setReportTypeName(result.getReportTypeName());
        dto.setScannerVendor(result.getScannerVendor());
        dto.setScannerProduct(result.getScannerProduct());
        dto.setSubTaskId(result.getSubTaskId());
        dto.setFileName(result.getFileName());
        dto.setFileFormat(result.getFileFormat());
        dto.setContentType(result.getContentType());
        dto.setByteSize(result.getByteSize());
        dto.setChecksum(result.getChecksum());
        dto.setStatus(result.getStatus());
        dto.setGeneratedAt(formatUtc(result.getGeneratedAt()));
        dto.setExpiresAt(formatUtc(result.getExpiresAt()));
        dto.setDownloadUrl(result.getDownloadUrl());
        dto.setErrorMessage(result.getErrorMessage());
        return dto;
    }

    public ArtifactListPageDto toPageDto(ArtifactListResult result) {
        ArtifactListPageDto dto = new ArtifactListPageDto();
        dto.setPage(result.getPage());
        dto.setSize(result.getSize());
        dto.setTotal(result.getTotal());
        if (!CollectionUtils.isEmpty(result.getItems())) {
            List<ArtifactMetadataDto> items = result.getItems().stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
            dto.setItems(items);
        }
        return dto;
    }

    private static String formatUtc(Date date) {
        if (date == null) {
            return null;
        }
        return ISO_UTC.format(date.toInstant());
    }
}
