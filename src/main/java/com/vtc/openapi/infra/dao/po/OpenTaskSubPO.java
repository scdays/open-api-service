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
@TableName("open_task_sub")
public class OpenTaskSubPO extends BasePO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String subId;
    private String taskId;
    private String partnerId;
    private Integer scanPhase;
    private String scannerType;
    private String centerTaskType;
    private String centerPlanId;
    private String surveyId;
    private String status;
    private Integer progress;
    private String errorMessage;
    private String reportDownloadPath;
    private Date createdAt;
    private Date updatedAt;
}
