package com.vtc.openapi.ui.dto.open.artifact;

import lombok.Data;

@Data
public class ArtifactMetadataDto {

    private String artifactId;
    private String taskId;
    private String extTaskId;
    private String exportId;
    private String exportStage;
    private String artifactSource;
    private Integer reportTypeCode;
    private String reportTypeName;
    private String scannerVendor;
    private String scannerProduct;
    private String subTaskId;
    private String fileName;
    private String fileFormat;
    private String contentType;
    private Long byteSize;
    private String checksum;
    private String status;
    private String generatedAt;
    private String expiresAt;
    private String downloadUrl;
    private String errorMessage;
}
