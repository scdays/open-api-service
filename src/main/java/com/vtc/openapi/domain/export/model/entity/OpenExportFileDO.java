package com.vtc.openapi.domain.export.model.entity;

import com.botany.spore.ddd.domain.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class OpenExportFileDO extends BaseDO {

    private Long id;
    private String exportId;
    private String realTaskId;
    private String partnerId;
    private String filePosition;
    private String fileField;
    private String fileMetadata;
    private Integer fileType;
    private Date createTime;
    private Date updateTime;
}
