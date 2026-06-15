package com.vtc.openapi.domain.export.model.convert;

import com.botany.spore.ddd.domain.model.convertor.IDomainConvertor;
import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.infra.dao.po.OpenExportPO;
import org.springframework.stereotype.Component;

@Component
public class OpenExportDomainConvertor implements IDomainConvertor<OpenExportDO, OpenExportPO> {

    @Override
    public OpenExportPO doToPo(OpenExportDO domainObject) {
        return IDomainConvertor.super.doToPo(domainObject);
    }

    @Override
    public OpenExportDO poToDo(OpenExportPO po) {
        return IDomainConvertor.super.poToDo(po);
    }
}
