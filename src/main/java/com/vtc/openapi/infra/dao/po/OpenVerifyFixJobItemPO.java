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
@TableName("open_verify_fix_job_item")
public class OpenVerifyFixJobItemPO extends BasePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String jobId;
    private String partnerId;
    private String vulInfoId;
    private String taskId;
    private Integer previousStat;
    private Integer resultStat;
    private String itemStatus;
    private Boolean rescanMatched;
    private String rescanSubId;
    private Date createdAt;
    private Date updatedAt;
}
