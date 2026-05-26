package com.vtc.openapi.domain.partner.model.convert;

import com.botany.spore.ddd.domain.model.convertor.IDomainConvertor;
import com.vtc.openapi.domain.partner.model.entity.PartnerDO;
import com.vtc.openapi.infra.dao.po.PartnerPO;
import org.springframework.stereotype.Component;

/**
 * Partner DO ↔ PO 转换（ConvertHelper 注册）。
 */
@Component
public class PartnerDomainConvertor implements IDomainConvertor<PartnerDO, PartnerPO> {

    @Override
    public PartnerPO doToPo(PartnerDO domainObject) {
        return IDomainConvertor.super.doToPo(domainObject);
    }

    @Override
    public PartnerDO poToDo(PartnerPO po) {
        return IDomainConvertor.super.poToDo(po);
    }
}
