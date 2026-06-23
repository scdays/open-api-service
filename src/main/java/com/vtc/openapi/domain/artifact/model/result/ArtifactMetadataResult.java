package com.vtc.openapi.domain.artifact.model.result;

import lombok.Data;

import java.util.Date;

@Data
public class ArtifactMetadataResult {

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
    private Date generatedAt;
    private Date expiresAt;
    private String downloadUrl;
    private String errorMessage;
}
