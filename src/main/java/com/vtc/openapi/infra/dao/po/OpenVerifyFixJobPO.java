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
@TableName("open_verify_fix_job")
public class OpenVerifyFixJobPO extends BasePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String jobId;
    private String partnerId;
    private String batchId;
    private String status;
    private Integer itemCount;
    private String errorMessage;
    private Boolean rescanImported;
    private String centerSubId;
    private String centerPlanId;
    private String surveyId;
    private String scannerType;
    private String inputIps;
    private Integer progress;
    private Date finishedAt;
    private Date createdAt;
    private Date updatedAt;
}
