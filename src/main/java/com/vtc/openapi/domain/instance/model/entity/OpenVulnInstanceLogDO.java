package com.vtc.openapi.domain.instance.model.entity;

import com.botany.spore.ddd.domain.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class OpenVulnInstanceLogDO extends BaseDO {

    public static final String REASON_SURVEY_INGEST = "SURVEY_INGEST";
    public static final String REASON_CROSS_SCAN_MERGE = "CROSS_SCAN_MERGE";
    public static final String REASON_VERIFY_PHASE = "VERIFY_PHASE";
    public static final String REASON_PARTNER_VERIFY = "PARTNER_VERIFY";
    public static final String REASON_PARTNER_REMEDIATE = "PARTNER_REMEDIATE";
    public static final String REASON_VERIFY_FIX_COMPLETE = "VERIFY_FIX_COMPLETE";

    private Long id;
    private String partnerId;
    private String vulInfoId;
    private String taskId;
    private String subId;
    private Integer scanPhase;
    private Integer prevStat;
    private Integer vulInfoStat;
    private String changeReason;
    private String verifyMergeStrategy;
    private Integer scannerHitCount;
    private String transferTime;
    private String caseId;
    private Date createdAt;
}
