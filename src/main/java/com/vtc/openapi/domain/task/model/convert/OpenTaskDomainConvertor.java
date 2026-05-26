package com.vtc.openapi.domain.task.model.convert;

import com.botany.spore.ddd.domain.model.convertor.IDomainConvertor;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.infra.dao.po.OpenTaskPO;
import org.springframework.stereotype.Component;

@Component
public class OpenTaskDomainConvertor implements IDomainConvertor<OpenTaskDO, OpenTaskPO> {

    @Override
    public OpenTaskPO doToPo(OpenTaskDO domainObject) {
        return IDomainConvertor.super.doToPo(domainObject);
    }

    @Override
    public OpenTaskDO poToDo(OpenTaskPO po) {
        return IDomainConvertor.super.poToDo(po);
    }
}
