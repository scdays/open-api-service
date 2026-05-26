package com.vtc.openapi.domain.open.model.convert;

import com.botany.spore.ddd.domain.model.convertor.IDomainConvertor;
import com.vtc.openapi.domain.open.model.entity.ApiInvocationDO;
import com.vtc.openapi.infra.dao.po.ApiInvocationPO;
import org.springframework.stereotype.Component;

@Component
public class ApiInvocationDomainConvertor implements IDomainConvertor<ApiInvocationDO, ApiInvocationPO> {

    @Override
    public ApiInvocationPO doToPo(ApiInvocationDO domainObject) {
        return IDomainConvertor.super.doToPo(domainObject);
    }

    @Override
    public ApiInvocationDO poToDo(ApiInvocationPO po) {
        return IDomainConvertor.super.poToDo(po);
    }
}
