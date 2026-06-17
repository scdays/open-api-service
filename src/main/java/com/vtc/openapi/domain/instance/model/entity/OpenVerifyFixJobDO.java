package com.vtc.openapi.domain.instance.model.entity;

import com.botany.spore.ddd.domain.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class OpenVerifyFixJobDO extends BaseDO {

    private Long id;

    private String jobId;
    private String partnerId;
    private String batchId;
    private String status;
    private Integer itemCount;
    private String errorMessage;
    private Boolean rescanImported;
    private Date finishedAt;
    private Date createdAt;
    private Date updatedAt;
}
