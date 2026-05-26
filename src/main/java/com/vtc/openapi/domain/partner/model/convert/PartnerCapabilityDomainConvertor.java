package com.vtc.openapi.domain.partner.model.convert;

import com.botany.spore.ddd.domain.model.convertor.IDomainConvertor;
import com.vtc.openapi.domain.partner.model.entity.PartnerCapabilityDO;
import com.vtc.openapi.infra.dao.po.PartnerCapabilityPO;
import org.springframework.stereotype.Component;

@Component
public class PartnerCapabilityDomainConvertor implements IDomainConvertor<PartnerCapabilityDO, PartnerCapabilityPO> {

    @Override
    public PartnerCapabilityPO doToPo(PartnerCapabilityDO domainObject) {
        return IDomainConvertor.super.doToPo(domainObject);
    }

    @Override
    public PartnerCapabilityDO poToDo(PartnerCapabilityPO po) {
        return IDomainConvertor.super.poToDo(po);
    }
}
