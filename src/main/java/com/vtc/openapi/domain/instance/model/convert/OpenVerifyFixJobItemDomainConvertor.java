package com.vtc.openapi.domain.instance.model.convert;

import com.botany.spore.ddd.domain.model.convertor.IDomainConvertor;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobItemDO;
import com.vtc.openapi.infra.dao.po.OpenVerifyFixJobItemPO;
import org.springframework.stereotype.Component;

@Component
public class OpenVerifyFixJobItemDomainConvertor implements IDomainConvertor<OpenVerifyFixJobItemDO, OpenVerifyFixJobItemPO> {

    @Override
    public OpenVerifyFixJobItemPO doToPo(OpenVerifyFixJobItemDO domainObject) {
        return IDomainConvertor.super.doToPo(domainObject);
    }

    @Override
    public OpenVerifyFixJobItemDO poToDo(OpenVerifyFixJobItemPO po) {
        return IDomainConvertor.super.poToDo(po);
    }
}
