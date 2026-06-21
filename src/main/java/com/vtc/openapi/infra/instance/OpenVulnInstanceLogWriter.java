package com.vtc.openapi.infra.instance;

import com.vtc.openapi.domain.instance.model.audit.OpenVulnInstanceAudit;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceLogDO;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceLogRepository;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Component
public class OpenVulnInstanceLogWriter {

    private static final DateTimeFormatter TRANSFER_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.US).withZone(ZoneOffset.UTC);

    private final IOpenVulnInstanceLogRepository logRepository;

    public OpenVulnInstanceLogWriter(IOpenVulnInstanceLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public void writeStateChange(OpenVulnInstanceDO instance,
                                 Integer prevStat,
                                 int newStat,
                                 OpenVulnInstanceAudit audit) {
        if (instance == null || audit == null || !StringUtils.hasText(instance.getVulInfoId())) {
            return;
        }
        if (prevStat != null && prevStat == newStat) {
            return;
        }
        OpenVulnInstanceLogDO row = new OpenVulnInstanceLogDO();
        row.setPartnerId(instance.getPartnerId());
        row.setVulInfoId(instance.getVulInfoId());
        row.setTaskId(StringUtils.hasText(audit.getTaskId()) ? audit.getTaskId() : instance.getTaskId());
        row.setSubId(audit.getSubId());
        row.setScanPhase(audit.getScanPhase());
        row.setPrevStat(prevStat);
        row.setVulInfoStat(newStat);
        row.setChangeReason(audit.getChangeReason());
        row.setVerifyMergeStrategy(audit.getVerifyMergeStrategy());
        row.setScannerHitCount(audit.getScannerHitCount());
        row.setTransferTime(resolveTransferTime(audit.getTransferTime()));
        row.setCaseId(audit.getCaseId());
        row.setCreatedAt(new Date());
        logRepository.insertBatch(java.util.Collections.singletonList(row));
    }

    public void writeIngestBatch(OpenTaskDO task, List<OpenVulnInstanceDO> instances, String subId, int scanPhase) {
        writeIngestBatch(task, instances, subId, scanPhase, OpenVulnInstanceLogDO.REASON_SURVEY_INGEST);
    }

    public void writeIngestBatch(OpenTaskDO task, List<OpenVulnInstanceDO> instances, String subId,
                                 int scanPhase, String changeReason) {
        if (task == null || instances == null || instances.isEmpty()) {
            return;
        }
        List<OpenVulnInstanceLogDO> rows = new ArrayList<>();
        for (OpenVulnInstanceDO instance : instances) {
            if (instance == null || !StringUtils.hasText(instance.getVulInfoId())) {
                continue;
            }
            OpenVulnInstanceLogDO row = new OpenVulnInstanceLogDO();
            row.setPartnerId(task.getPartnerId());
            row.setVulInfoId(instance.getVulInfoId());
            row.setTaskId(task.getTaskId());
            row.setSubId(subId);
            row.setScanPhase(scanPhase);
            row.setPrevStat(null);
            row.setVulInfoStat(instance.getVulInfoStat() != null ? instance.getVulInfoStat() : 1);
            row.setChangeReason(changeReason);
            row.setTransferTime(resolveTransferTime(null));
            row.setCreatedAt(new Date());
            rows.add(row);
        }
        logRepository.insertBatch(rows);
    }

    private static String resolveTransferTime(String transferTime) {
        if (StringUtils.hasText(transferTime)) {
            return transferTime.trim();
        }
        return TRANSFER_TIME_FMT.format(new Date().toInstant());
    }
}
