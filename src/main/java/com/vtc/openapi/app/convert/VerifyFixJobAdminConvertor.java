package com.vtc.openapi.app.convert;

import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobItemDO;
import com.vtc.openapi.domain.instance.service.business.IVerifyFixJobDomainService;
import com.vtc.openapi.ui.dto.admin.MockVerifyFixJobDto;
import com.vtc.openapi.ui.dto.admin.MockVerifyFixJobItemDto;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class VerifyFixJobAdminConvertor {

    private static final DateTimeFormatter ISO_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private final IVerifyFixJobDomainService verifyFixJobDomainService;

    public VerifyFixJobAdminConvertor(IVerifyFixJobDomainService verifyFixJobDomainService) {
        this.verifyFixJobDomainService = verifyFixJobDomainService;
    }

    public MockVerifyFixJobDto toJobDto(OpenVerifyFixJobDO job, boolean withItems) {
        if (job == null) {
            return null;
        }
        MockVerifyFixJobDto dto = new MockVerifyFixJobDto();
        dto.setJobId(job.getJobId());
        dto.setPartnerId(job.getPartnerId());
        dto.setBatchId(job.getBatchId());
        dto.setCaseId(job.getCaseId());
        dto.setStatus(job.getStatus());
        dto.setItemCount(job.getItemCount());
        dto.setProgress(job.getProgress());
        dto.setRetryCount(job.getRetryCount());
        dto.setRescanImported(job.getRescanImported());
        dto.setErrorMessage(job.getErrorMessage());
        dto.setFinishedAt(formatUtc(job.getFinishedAt()));
        dto.setCreatedAt(formatUtc(job.getCreatedAt()));
        if (withItems) {
            List<OpenVerifyFixJobItemDO> items = verifyFixJobDomainService.listJobItems(job.getJobId());
            for (OpenVerifyFixJobItemDO item : items) {
                MockVerifyFixJobItemDto row = new MockVerifyFixJobItemDto();
                row.setVulInfoId(item.getVulInfoId());
                row.setTaskId(item.getTaskId());
                row.setPreviousStat(item.getPreviousStat());
                row.setResultStat(item.getResultStat());
                row.setItemStatus(item.getItemStatus());
                row.setSourceSubId(item.getSourceSubId());
                row.setScannerType(item.getScannerType());
                row.setRescanSubId(item.getRescanSubId());
                dto.getItems().add(row);
            }
        }
        return dto;
    }

    private static String formatUtc(java.util.Date date) {
        return date != null ? ISO_UTC.format(date.toInstant()) : null;
    }
}
