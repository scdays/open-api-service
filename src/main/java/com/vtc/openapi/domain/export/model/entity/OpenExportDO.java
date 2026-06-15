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
    private Date generatedAt;
    private Date createdAt;
    private Date updatedAt;
}
