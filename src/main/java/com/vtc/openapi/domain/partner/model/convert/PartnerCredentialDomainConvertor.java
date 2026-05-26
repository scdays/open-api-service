package com.vtc.openapi.domain.partner.model.convert;

import com.botany.spore.ddd.domain.model.convertor.IDomainConvertor;
import com.vtc.openapi.domain.partner.model.entity.PartnerCredentialDO;
import com.vtc.openapi.infra.dao.po.PartnerCredentialPO;
import org.springframework.stereotype.Component;

@Component
public class PartnerCredentialDomainConvertor implements IDomainConvertor<PartnerCredentialDO, PartnerCredentialPO> {

    @Override
    public PartnerCredentialPO doToPo(PartnerCredentialDO domainObject) {
        return IDomainConvertor.super.doToPo(domainObject);
    }

    @Override
    public PartnerCredentialDO poToDo(PartnerCredentialPO po) {
        return IDomainConvertor.super.poToDo(po);
    }
}
