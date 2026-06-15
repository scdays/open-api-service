package com.vtc.openapi.app.convert;

import com.vtc.openapi.domain.export.model.result.ExportListResult;
import com.vtc.openapi.domain.export.model.result.ExportMetadataResult;
import com.vtc.openapi.ui.dto.open.export.ExportListPageDto;
import com.vtc.openapi.ui.dto.open.export.ExportMetadataDto;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

@Component
public class OpenExportAppConvertor {

    private static final SimpleDateFormat ISO_UTC;

    static {
        ISO_UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        ISO_UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public ExportMetadataDto toDto(ExportMetadataResult result) {
        if (result == null) {
            return null;
        }
        ExportMetadataDto dto = new ExportMetadataDto();
        dto.setExportId(result.getExportId());
        dto.setTaskId(result.getTaskId());
        dto.setExtTaskId(result.getExtTaskId());
        dto.setReportTemplateId(result.getReportTemplateId());
        dto.setFormat(result.getFormat());
        dto.setExportStage(result.getExportStage());
        dto.setDataType(result.getDataType());
        dto.setStatus(result.getStatus());
        dto.setRecordCount(result.getRecordCount());
        dto.setExpiresAt(formatUtc(result.getExpiresAt()));
        dto.setCreatedAt(formatUtc(result.getCreatedAt()));
        dto.setDownloadUrl(result.getDownloadUrl());
        return dto;
    }

    public ExportListPageDto toPageDto(ExportListResult result) {
        ExportListPageDto dto = new ExportListPageDto();
        dto.setPage(result.getPage());
        dto.setSize(result.getSize());
        dto.setTotal(result.getTotal());
        if (!CollectionUtils.isEmpty(result.getItems())) {
            List<ExportMetadataDto> items = result.getItems().stream()
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
        synchronized (ISO_UTC) {
            return ISO_UTC.format(date);
        }
    }
}
