package com.vtc.openapi.domain.instance.model.entity;

import com.botany.spore.ddd.domain.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class OpenVerifyFixJobItemDO extends BaseDO {

    private Long id;

    private String jobId;
    private String partnerId;
    private String vulInfoId;
    private String taskId;
    private Integer previousStat;
    private Integer resultStat;
    private String itemStatus;
    private Date createdAt;
    private Date updatedAt;
}
