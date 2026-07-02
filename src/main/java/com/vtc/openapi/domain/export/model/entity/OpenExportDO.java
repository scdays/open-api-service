package com.vtc.openapi.domain.export.model.entity;

import com.botany.spore.ddd.domain.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
    /** 关联的 Webhook 事件ID（业务侧生成，用于关联投递记录与业务详情） */
    private String webhookEventId;
    private Date generatedAt;
    private Date createdAt;
    private Date updatedAt;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
