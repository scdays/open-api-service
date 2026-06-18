package com.vtc.openapi.infra.dao.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.botany.spore.mybatis.pojo.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("open_operation_case")
public class OpenOperationCasePO extends BasePO {

    @TableId
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
