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
@TableName("open_vuln_instance")
public class OpenVulnInstancePO extends BasePO {

    @TableId(type = IdType.AUTO)
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
