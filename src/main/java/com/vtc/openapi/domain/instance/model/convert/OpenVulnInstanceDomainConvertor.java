package com.vtc.openapi.domain.instance.model.convert;

import com.botany.spore.ddd.domain.model.convertor.IDomainConvertor;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.infra.dao.po.OpenVulnInstancePO;
import org.springframework.stereotype.Component;

@Component
public class OpenVulnInstanceDomainConvertor implements IDomainConvertor<OpenVulnInstanceDO, OpenVulnInstancePO> {

    @Override
    public OpenVulnInstancePO doToPo(OpenVulnInstanceDO domainObject) {
        return IDomainConvertor.super.doToPo(domainObject);
    }

    @Override
    public OpenVulnInstanceDO poToDo(OpenVulnInstancePO po) {
        return IDomainConvertor.super.poToDo(po);
    }
}
