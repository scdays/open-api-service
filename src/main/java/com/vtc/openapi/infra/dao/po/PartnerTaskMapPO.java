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
@TableName("partner_task_map")
public class PartnerTaskMapPO extends BasePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String partnerId;

    private String extTaskId;

    private String platformTaskId;

    private Date createdAt;
}
