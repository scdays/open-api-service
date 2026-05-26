package com.vtc.openapi.domain.open.model.convert;

import com.botany.spore.ddd.domain.model.convertor.IDomainConvertor;
import com.vtc.openapi.domain.open.model.entity.ApiOperationDO;
import com.vtc.openapi.infra.dao.po.ApiOperationPO;
import org.springframework.stereotype.Component;

@Component
public class ApiOperationDomainConvertor implements IDomainConvertor<ApiOperationDO, ApiOperationPO> {

    @Override
    public ApiOperationPO doToPo(ApiOperationDO domainObject) {
        return IDomainConvertor.super.doToPo(domainObject);
    }

    @Override
    public ApiOperationDO poToDo(ApiOperationPO po) {
        return IDomainConvertor.super.poToDo(po);
    }
}
