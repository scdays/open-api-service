package com.vtc.openapi.domain.partner.model.entity;

import com.botany.spore.ddd.domain.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class PartnerCapabilityDO extends BaseDO {

    private Long id;

    private String partnerId;

    private String capability;

    private Date createdAt;
}
