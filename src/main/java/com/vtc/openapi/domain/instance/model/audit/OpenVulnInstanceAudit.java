package com.vtc.openapi.domain.instance.model.audit;

import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceLogDO;
import lombok.Getter;

/**
 * 实例状态变更审计上下文（经 {@link OpenVulnInstanceAuditContext} 传入仓储层）。
 */
@Getter
public class OpenVulnInstanceAudit {

    private final String changeReason;
    private final String transferTime;
    private String taskId;
    private String subId;
    private Integer scanPhase;
    private String verifyMergeStrategy;
    private Integer scannerHitCount;
    private String caseId;

    private OpenVulnInstanceAudit(String changeReason, String transferTime) {
        this.changeReason = changeReason;
        this.transferTime = transferTime;
    }

    public static OpenVulnInstanceAudit partnerVerify(String transferTime) {
        return new OpenVulnInstanceAudit(OpenVulnInstanceLogDO.REASON_PARTNER_VERIFY, transferTime);
    }

    public static OpenVulnInstanceAudit partnerRemediate(String transferTime) {
        return new OpenVulnInstanceAudit(OpenVulnInstanceLogDO.REASON_PARTNER_REMEDIATE, transferTime);
    }

    public static OpenVulnInstanceAudit verifyFixComplete(String jobId) {
        OpenVulnInstanceAudit audit = new OpenVulnInstanceAudit(
                OpenVulnInstanceLogDO.REASON_VERIFY_FIX_COMPLETE, null);
        audit.subId = jobId;
        return audit;
    }

    public static OpenVulnInstanceAudit verifyPhase(String subId,
                                                    String verifyMergeStrategy,
                                                    Integer scannerHitCount) {
        OpenVulnInstanceAudit audit = new OpenVulnInstanceAudit(
                OpenVulnInstanceLogDO.REASON_VERIFY_PHASE, null);
        audit.subId = subId;
        audit.scanPhase = 2;
        audit.verifyMergeStrategy = verifyMergeStrategy;
        audit.scannerHitCount = scannerHitCount;
        return audit;
    }

    /** 交叉扫描：基于排查阶段双扫结果合并，不再二次下发 VTC 任务。 */
    public static OpenVulnInstanceAudit crossScanMerge(String subId,
                                                       String verifyMergeStrategy,
                                                       Integer scannerHitCount) {
        OpenVulnInstanceAudit audit = new OpenVulnInstanceAudit(
                OpenVulnInstanceLogDO.REASON_CROSS_SCAN_MERGE, null);
        audit.subId = subId;
        audit.scanPhase = 1;
        audit.verifyMergeStrategy = verifyMergeStrategy;
        audit.scannerHitCount = scannerHitCount;
        return audit;
    }

    public OpenVulnInstanceAudit taskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    public OpenVulnInstanceAudit subId(String subId) {
        this.subId = subId;
        return this;
    }

    public OpenVulnInstanceAudit scanPhase(Integer scanPhase) {
        this.scanPhase = scanPhase;
        return this;
    }

    public OpenVulnInstanceAudit caseId(String caseId) {
        this.caseId = caseId;
        return this;
    }
}
