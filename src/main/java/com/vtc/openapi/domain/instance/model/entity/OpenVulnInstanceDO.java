package com.vtc.openapi.domain.instance.model.entity;

import com.botany.spore.ddd.domain.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class OpenVulnInstanceDO extends BaseDO {

    private Long id;

    private String partnerId;

    private String vulInfoId;

    private String vulnDisposalId;

    private String engineTaskId;

    private String taskId;

    private String extTaskId;

    private Integer scanTemplateId;

    private Integer reportTemplateId;

    private String bundleId;

    private String ingestStatus;

    private Date ingestAt;

    private Integer vulInfoStat;

    private String snapshotJson;

    private Date createdAt;

    private Date updatedAt;
}
