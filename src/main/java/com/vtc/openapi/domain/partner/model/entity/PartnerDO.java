package com.vtc.openapi.domain.partner.model.entity;

import com.botany.spore.ddd.domain.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Partner 主数据（领域对象）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PartnerDO extends BaseDO {

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
