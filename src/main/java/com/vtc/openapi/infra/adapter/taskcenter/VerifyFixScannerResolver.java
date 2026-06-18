package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceLogDO;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceLogRepository;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskSubRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 修复核验扫描器选举：open_vuln_instance_log.sub_id → open_task_sub.scanner_type。
 */
@Component
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class VerifyFixScannerResolver {

    private static final Set<String> SCAN_LOG_REASONS = new HashSet<>(Arrays.asList(
            OpenVulnInstanceLogDO.REASON_SURVEY_INGEST,
            OpenVulnInstanceLogDO.REASON_VERIFY_PHASE,
            OpenVulnInstanceLogDO.REASON_CROSS_SCAN_MERGE
    ));

    private final IOpenVulnInstanceLogRepository logRepository;
    private final IOpenTaskSubRepository taskSubRepository;

    public VerifyFixScannerResolver(IOpenVulnInstanceLogRepository logRepository,
                                    IOpenTaskSubRepository taskSubRepository) {
        this.logRepository = logRepository;
        this.taskSubRepository = taskSubRepository;
    }

    public ResolveResult resolve(String partnerId, String vulInfoId, String taskId) {
        if (!StringUtils.hasText(partnerId) || !StringUtils.hasText(vulInfoId)) {
            return ResolveResult.unresolved("partnerId/vulInfoId 为空");
        }
        List<OpenVulnInstanceLogDO> logs = logRepository.listByVulInfoId(
                partnerId.trim(), vulInfoId.trim(), 80);
        for (OpenVulnInstanceLogDO row : logs) {
            if (row == null || !StringUtils.hasText(row.getSubId())) {
                continue;
            }
            if (!SCAN_LOG_REASONS.contains(row.getChangeReason())) {
                continue;
            }
            if (StringUtils.hasText(taskId) && !taskId.trim().equals(row.getTaskId())) {
                continue;
            }
            OpenTaskSubDO sourceSub = taskSubRepository.findBySubId(row.getSubId().trim());
            if (sourceSub == null || !StringUtils.hasText(sourceSub.getScannerType())) {
                continue;
            }
            ResolveResult result = new ResolveResult();
            result.resolved = true;
            result.scannerType = sourceSub.getScannerType().trim();
            result.sourceSubId = row.getSubId().trim();
            return result;
        }
        return ResolveResult.unresolved("未找到带 sub_id 的扫描跃迁日志");
    }

    public static final class ResolveResult {
        public boolean resolved;
        public String scannerType;
        public String sourceSubId;
        public String errorMessage;

        public static ResolveResult unresolved(String message) {
            ResolveResult r = new ResolveResult();
            r.resolved = false;
            r.errorMessage = message;
            return r;
        }
    }
}
