package com.vtc.openapi.infra.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.botany.spore.mybatis.pojo.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("open_artifact")
public class OpenArtifactPO extends BasePO {

    @TableId(type = IdType.AUTO)
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
    private String webhookEventId;
    private Date createdAt;
    private Date updatedAt;
}
