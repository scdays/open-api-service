package com.vtc.openapi.domain.task.model.convert;

import com.botany.spore.ddd.domain.model.convertor.IDomainConvertor;
import com.vtc.openapi.domain.task.model.entity.OpenTaskScanResultDO;
import com.vtc.openapi.infra.dao.po.OpenTaskScanResultPO;
import org.springframework.stereotype.Component;

/**
 * OpenTaskScanResult DO ↔ PO 转换（ConvertHelper 注册）。
 */
@Component
public class OpenTaskScanResultDomainConvertor implements IDomainConvertor<OpenTaskScanResultDO, OpenTaskScanResultPO> {

    @Override
    public OpenTaskScanResultPO doToPo(OpenTaskScanResultDO domainObject) {
        return IDomainConvertor.super.doToPo(domainObject);
    }

    @Override
    public OpenTaskScanResultDO poToDo(OpenTaskScanResultPO po) {
        return IDomainConvertor.super.poToDo(po);
    }
}
