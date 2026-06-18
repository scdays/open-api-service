package com.vtc.openapi.domain.operationcase.model.entity;

import com.botany.spore.ddd.domain.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class OpenOperationCaseEventDO extends BaseDO {

    private Long id;
    private String caseId;
    private String eventType;
    private String eventPayloadJson;
    private Date createdAt;
}
