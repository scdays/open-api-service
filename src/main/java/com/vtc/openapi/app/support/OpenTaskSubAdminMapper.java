package com.vtc.openapi.app.support;

import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.ui.dto.admin.OpenTaskSubDto;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class OpenTaskSubAdminMapper {

    private static final DateTimeFormatter ISO_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    public OpenTaskSubDto toDto(OpenTaskSubDO sub) {
        if (sub == null) {
            return null;
        }
        OpenTaskSubDto dto = new OpenTaskSubDto();
        dto.setSubId(sub.getSubId());
        dto.setTaskId(sub.getTaskId());
        dto.setScanPhase(sub.getScanPhase());
        dto.setScannerType(sub.getScannerType());
        dto.setScannerLabel(resolveScannerLabel(sub.getScannerType()));
        dto.setCenterTaskType(sub.getCenterTaskType());
        dto.setCenterPlanId(sub.getCenterPlanId());
        dto.setSurveyId(sub.getSurveyId());
        dto.setStatus(sub.getStatus());
        dto.setProgress(sub.getProgress());
        dto.setErrorMessage(sub.getErrorMessage());
        dto.setReportDownloadPath(sub.getReportDownloadPath());
        dto.setVerifyFixJobId(sub.getVerifyFixJobId());
        dto.setCreatedAt(formatUtc(sub.getCreatedAt()));
        dto.setUpdatedAt(formatUtc(sub.getUpdatedAt()));
        return dto;
    }

    public static String resolveScannerLabel(String scannerType) {
        if ("1".equals(scannerType)) {
            return "绿盟 RSAS";
        }
        if ("7".equals(scannerType)) {
            return "Nessus";
        }
        return scannerType != null ? "scanner-" + scannerType : "-";
    }

    private static String formatUtc(java.util.Date date) {
        return date != null ? ISO_UTC.format(date.toInstant()) : null;
    }
}
