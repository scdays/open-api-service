package com.vtc.openapi.domain.export.model.result;

import lombok.Data;

import java.util.Date;

@Data
public class ExportMetadataResult {

    private String exportId;
    private String taskId;
    private String extTaskId;
    private Integer reportTemplateId;
    private String format;
    private String exportStage;
    private String dataType;
    private String status;
    private Integer recordCount;
    private Date expiresAt;
    private Date createdAt;
    private String downloadUrl;
}
