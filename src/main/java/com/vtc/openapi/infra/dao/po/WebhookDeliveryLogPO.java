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
@TableName("webhook_delivery_log")
public class WebhookDeliveryLogPO extends BasePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String partnerId;

    private String eventType;

    private String payloadJson;

    private String callbackUrl;

    private Integer httpStatus;

    private Integer retryCount;

    private String status;

    private Date createdAt;

    private Date nextRetryAt;
}
