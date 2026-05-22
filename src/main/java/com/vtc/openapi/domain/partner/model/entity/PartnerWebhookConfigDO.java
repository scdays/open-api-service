package com.vtc.openapi.domain.partner.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("partner_webhook_config")
public class PartnerWebhookConfigDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String partnerId;

    private String callbackUrl;

    /** HMAC 密钥哈希 */
    private String webhookSecretHash;

    private Date updatedAt;
}
