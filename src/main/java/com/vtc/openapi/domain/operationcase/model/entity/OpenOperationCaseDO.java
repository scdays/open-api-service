package com.vtc.openapi.domain.operationcase.model.entity;

import com.botany.spore.ddd.domain.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class OpenOperationCaseDO extends BaseDO {

    private String caseId;
    private String partnerId;
    private String caseType;
    private String status;
    private String title;
    private String primaryResourceType;
    private String primaryResourceId;
    private String batchId;
    private String invocationId;
    private String idempotencyKey;
    private String requestSummaryJson;
    private String resultSummaryJson;
    private String errorMessage;
    private Date startedAt;
    private Date finishedAt;
    private Date createdAt;
    private Date updatedAt;
}
