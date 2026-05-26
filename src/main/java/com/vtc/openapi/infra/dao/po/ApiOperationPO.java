package com.vtc.openapi.infra.dao.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.botany.spore.mybatis.pojo.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("api_operation")
public class ApiOperationPO extends BasePO {

    @TableId
    private String operationId;

    private String apiVersion;

    private String httpMethod;

    private String pathPattern;

    private String requiredCapability;

    private String domain;

    private String status;

    private Date publishedAt;

    private String openapiTag;

    private String summary;
}
