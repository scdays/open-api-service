package com.vtc.openapi.infra.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.botany.spore.mybatis.pojo.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("api_invocation")
public class ApiInvocationPO extends BasePO {

    @TableId
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

    @TableField(select = false)
    private String responseBodyJson;

    private Date startedAt;

    private Date finishedAt;
}
