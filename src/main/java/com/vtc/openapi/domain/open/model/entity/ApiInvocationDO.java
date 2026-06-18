package com.vtc.openapi.domain.open.model.entity;

import com.botany.spore.ddd.domain.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class ApiInvocationDO extends BaseDO {

    private String invocationId;

    private String requestId;

    private String partnerId;

    private String operationId;

    private String httpMethod;

    private String requestPath;

    private Integer responseCode;

    private Integer httpStatus;

    private Integer latencyMs;

    private String clientIp;

    private String errorMessage;

    private String resourceType;

    private String resourceId;

    private String caseId;

    private String responseBodyJson;

    private String requestBodyJson;

    private Date startedAt;

    private Date finishedAt;
}
