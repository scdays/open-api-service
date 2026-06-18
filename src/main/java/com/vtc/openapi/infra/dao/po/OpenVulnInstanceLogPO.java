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
@TableName("open_vuln_instance_log")
public class OpenVulnInstanceLogPO extends BasePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String partnerId;
    private String vulInfoId;
    private String taskId;
    private String subId;
    private Integer scanPhase;
    private Integer prevStat;
    private Integer vulInfoStat;
    private String changeReason;
    private String verifyMergeStrategy;
    private Integer scannerHitCount;
    private String transferTime;
    private String caseId;
    private Date createdAt;
}
