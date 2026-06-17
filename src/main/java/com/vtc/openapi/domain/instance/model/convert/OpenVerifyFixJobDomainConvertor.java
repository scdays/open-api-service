package com.vtc.openapi.domain.instance.model.convert;

import com.botany.spore.ddd.domain.model.convertor.IDomainConvertor;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobDO;
import com.vtc.openapi.infra.dao.po.OpenVerifyFixJobPO;
import org.springframework.stereotype.Component;

@Component
public class OpenVerifyFixJobDomainConvertor implements IDomainConvertor<OpenVerifyFixJobDO, OpenVerifyFixJobPO> {

    @Override
    public OpenVerifyFixJobPO doToPo(OpenVerifyFixJobDO domainObject) {
        return IDomainConvertor.super.doToPo(domainObject);
    }

    @Override
    public OpenVerifyFixJobDO poToDo(OpenVerifyFixJobPO po) {
        return IDomainConvertor.super.poToDo(po);
    }
}
