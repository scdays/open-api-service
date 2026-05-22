package com.vtc.openapi.domain.partner.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Partner 主数据。
 */
@Data
@TableName("partner")
public class PartnerDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String partnerId;

    private String partnerName;

    private String partnerType;

    /** ACTIVE / DISABLED */
    private String status;

    private Integer rateLimitQps;

    private Date createdAt;

    private Date updatedAt;
}
