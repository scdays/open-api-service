package com.vtc.openapi.domain.operationcase.model.convert;

import com.botany.spore.ddd.domain.model.convertor.IDomainConvertor;
import com.vtc.openapi.domain.operationcase.model.entity.OpenOperationCaseDO;
import com.vtc.openapi.infra.dao.po.OpenOperationCasePO;
import org.springframework.stereotype.Component;

/**
 * OpenOperationCase DO ↔ PO 转换（ConvertHelper 注册）。
 */
@Component
public class OpenOperationCaseDomainConvertor implements IDomainConvertor<OpenOperationCaseDO, OpenOperationCasePO> {

    @Override
    public OpenOperationCasePO doToPo(OpenOperationCaseDO domainObject) {
        return IDomainConvertor.super.doToPo(domainObject);
    }

    @Override
    public OpenOperationCaseDO poToDo(OpenOperationCasePO po) {
        return IDomainConvertor.super.poToDo(po);
    }
}
