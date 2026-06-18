package com.vtc.openapi.domain.instance.model.entity;

import lombok.Data;

import java.util.Date;

@Data
public class OpenVulnInstanceLogDO {

    public static final String REASON_SURVEY_INGEST = "SURVEY_INGEST";
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
    private Date createdAt;
}
