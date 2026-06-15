package com.vtc.openapi.domain.partner.model.entity;

import com.botany.spore.ddd.domain.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@Component
@Scope("prototype")
public class PartnerWebhookConfigDO extends BaseDO {

    private Long id;

    private String partnerId;

    private String callbackUrl;

    /** HMAC 密钥哈希 */
    private String webhookSecretHash;

    /** HMAC-SHA256 验签明文密钥（运营分配） */
    private String webhookSecret;

    private Date updatedAt;
}
