package com.vtc.openapi.domain.task.model.entity;

import com.botany.spore.ddd.domain.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class OpenTaskScanResultDO extends BaseDO {

    public static final String TYPE_LIVE_PROBE = "LIVE_PROBE";
    public static final String TYPE_PORT_SCAN = "PORT_SCAN";
    /** VTC vulnScanResultList 单行（原始 JSON） */
    public static final String TYPE_VULN_SCAN = "VULN_SCAN";
    /** VTC vulnDatabaseList 整包（单条 meta 行） */
    public static final String TYPE_VULN_DATABASE = "VULN_DATABASE";
    public static final String VULN_DATABASE_META_KEY = "@vulnDatabaseList";

    private Long id;
    private String taskId;
    private String subId;
    private String partnerId;
    private Integer scanPhase;
    private String surveyId;
    private String scannerType;
    private String resultType;
    private String resultKey;
    private String payloadJson;
    private Date createdAt;
    private Date updatedAt;
}
