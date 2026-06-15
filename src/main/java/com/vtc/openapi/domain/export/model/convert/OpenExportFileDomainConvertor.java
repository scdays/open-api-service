package com.vtc.openapi.domain.export.model.convert;

import com.botany.spore.ddd.domain.model.convertor.IDomainConvertor;
import com.vtc.openapi.domain.export.model.entity.OpenExportFileDO;
import com.vtc.openapi.infra.dao.po.OpenExportFilePO;
import org.springframework.stereotype.Component;

@Component
public class OpenExportFileDomainConvertor implements IDomainConvertor<OpenExportFileDO, OpenExportFilePO> {

    @Override
    public OpenExportFilePO doToPo(OpenExportFileDO domainObject) {
        return IDomainConvertor.super.doToPo(domainObject);
    }

    @Override
    public OpenExportFileDO poToDo(OpenExportFilePO po) {
        return IDomainConvertor.super.poToDo(po);
    }
}
