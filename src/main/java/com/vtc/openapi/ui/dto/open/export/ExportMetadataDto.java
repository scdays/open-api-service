package com.vtc.openapi.ui.dto.open.export;

import lombok.Data;

@Data
public class ExportMetadataDto {

    private String exportId;
    private String taskId;
    private String extTaskId;
    private Integer reportTemplateId;
    private String format;
    private String exportStage;
    private String dataType;
    private String status;
    private Integer recordCount;
    private String expiresAt;
    private String createdAt;
    private String downloadUrl;
}
