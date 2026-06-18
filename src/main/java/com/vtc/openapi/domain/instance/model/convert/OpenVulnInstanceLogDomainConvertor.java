package com.vtc.openapi.domain.instance.model.convert;

import com.botany.spore.ddd.domain.model.convertor.IDomainConvertor;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceLogDO;
import com.vtc.openapi.infra.dao.po.OpenVulnInstanceLogPO;
import org.springframework.stereotype.Component;

/**
 * OpenVulnInstanceLog DO ↔ PO 转换（ConvertHelper 注册）。
 */
@Component
public class OpenVulnInstanceLogDomainConvertor implements IDomainConvertor<OpenVulnInstanceLogDO, OpenVulnInstanceLogPO> {

    @Override
    public OpenVulnInstanceLogPO doToPo(OpenVulnInstanceLogDO domainObject) {
        return IDomainConvertor.super.doToPo(domainObject);
    }

    @Override
    public OpenVulnInstanceLogDO poToDo(OpenVulnInstanceLogPO po) {
        return IDomainConvertor.super.poToDo(po);
    }
}
