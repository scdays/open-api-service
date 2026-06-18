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
@TableName("open_task")
public class OpenTaskPO extends BasePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;

    private String partnerId;

    private String extTaskId;

    private String engineTaskId;

    private String taskName;

    private String targetType;

    private Integer vulnType;

    private String targetsJson;

    private String status;

    private Integer progress;

    private Integer scanTemplateId;

    private Integer reportTemplateId;

    private String callbackUrl;

    private String optionsJson;

    private String errorMessage;

    private Date startedAt;

    private Date finishedAt;

    private Boolean instancesIngested;

    private String ingestError;

    private Date createdAt;

    private Date updatedAt;

    private Integer taskPhase;

    private Boolean autoVerify;

    private String verifyMergeStrategy;

    private Boolean crossScan;
}
