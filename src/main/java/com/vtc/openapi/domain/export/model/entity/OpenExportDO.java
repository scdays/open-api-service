package com.vtc.openapi.domain.export.model.entity;

import com.botany.spore.ddd.domain.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class OpenExportDO extends BaseDO {

    private Long id;
    private String exportId;
    private String partnerId;
    private String taskId;
    private String extTaskId;
    private Integer reportTemplateId;
    private String format;
    private String exportStage;
    private String dataType;
    private String status;
    private Integer recordCount;
    private Date expiresAt;
    private String storagePath;
    private String downloadUrl;
    private String errorMessage;
    private String verifyFixJobId;
    /** 原始报告归档关联子任务 ID；外发记录为 null */
    private String subId;
    private Date generatedAt;
    private Date createdAt;
    private Date updatedAt;
}
