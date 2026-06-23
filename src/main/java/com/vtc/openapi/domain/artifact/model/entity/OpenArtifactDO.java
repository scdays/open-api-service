package com.vtc.openapi.domain.artifact.model.entity;

import com.botany.spore.ddd.domain.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class OpenArtifactDO extends BaseDO {

    private Long id;
    private String artifactId;
    private String partnerId;
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
    private String filePosition;
    private String fileField;
    private Date createdAt;
    private Date updatedAt;
}
