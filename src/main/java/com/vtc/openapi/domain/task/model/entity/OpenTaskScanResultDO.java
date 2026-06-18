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
