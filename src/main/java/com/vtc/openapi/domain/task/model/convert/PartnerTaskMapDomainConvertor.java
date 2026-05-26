package com.vtc.openapi.domain.task.model.convert;

import com.botany.spore.ddd.domain.model.convertor.IDomainConvertor;
import com.vtc.openapi.domain.task.model.entity.PartnerTaskMapDO;
import com.vtc.openapi.infra.dao.po.PartnerTaskMapPO;
import org.springframework.stereotype.Component;

@Component
public class PartnerTaskMapDomainConvertor implements IDomainConvertor<PartnerTaskMapDO, PartnerTaskMapPO> {

    @Override
    public PartnerTaskMapPO doToPo(PartnerTaskMapDO domainObject) {
        return IDomainConvertor.super.doToPo(domainObject);
    }

    @Override
    public PartnerTaskMapDO poToDo(PartnerTaskMapPO po) {
        return IDomainConvertor.super.poToDo(po);
    }
}
