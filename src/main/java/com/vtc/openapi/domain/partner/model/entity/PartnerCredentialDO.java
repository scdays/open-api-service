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
public class PartnerCredentialDO extends BaseDO {

    private Long id;

    private String partnerId;

    private String clientId;

    /** secret 哈希，明文仅创建时返回一次 */
    private String clientSecretHash;

    private String status;

    private Date expiresAt;

    private Date createdAt;

    private Date updatedAt;
}
