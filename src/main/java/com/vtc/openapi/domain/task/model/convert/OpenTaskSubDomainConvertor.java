package com.vtc.openapi.domain.task.model.convert;

import com.botany.spore.ddd.domain.model.convertor.IDomainConvertor;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.infra.dao.po.OpenTaskSubPO;
import org.springframework.stereotype.Component;

@Component
public class OpenTaskSubDomainConvertor implements IDomainConvertor<OpenTaskSubDO, OpenTaskSubPO> {

    @Override
    public OpenTaskSubPO doToPo(OpenTaskSubDO domainObject) {
        return IDomainConvertor.super.doToPo(domainObject);
    }

    @Override
    public OpenTaskSubDO poToDo(OpenTaskSubPO po) {
        return IDomainConvertor.super.poToDo(po);
    }
}
