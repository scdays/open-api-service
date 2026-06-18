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
@TableName("open_task_scan_result")
public class OpenTaskScanResultPO extends BasePO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String subId;
    private String partnerId;
    private Integer scanPhase;
    private String surveyId;
    private String scannerType;
    private String resultType;
    private String resultKey;
    private String payloadJson;
    private Date createdAt;
    private Date updatedAt;
}
