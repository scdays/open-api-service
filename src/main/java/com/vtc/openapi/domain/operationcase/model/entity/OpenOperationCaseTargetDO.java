package com.vtc.openapi.domain.operationcase.model.entity;

import com.botany.spore.ddd.domain.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class OpenOperationCaseTargetDO extends BaseDO {

    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PENDING = "PENDING";

    private Long id;
    private String caseId;
    private String targetKey;
    private String targetStatus;
    private Integer prevStat;
    private Integer resultStat;
    private String payloadJson;
    private Date createdAt;
}
