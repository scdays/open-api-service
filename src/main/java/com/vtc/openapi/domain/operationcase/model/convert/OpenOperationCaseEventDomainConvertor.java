package com.vtc.openapi.domain.operationcase.model.convert;

import com.botany.spore.ddd.domain.model.convertor.IDomainConvertor;
import com.vtc.openapi.domain.operationcase.model.entity.OpenOperationCaseEventDO;
import com.vtc.openapi.infra.dao.po.OpenOperationCaseEventPO;
import org.springframework.stereotype.Component;

/**
 * OpenOperationCaseEvent DO ↔ PO 转换（ConvertHelper 注册）。
 */
@Component
public class OpenOperationCaseEventDomainConvertor
        implements IDomainConvertor<OpenOperationCaseEventDO, OpenOperationCaseEventPO> {

    @Override
    public OpenOperationCaseEventPO doToPo(OpenOperationCaseEventDO domainObject) {
        return IDomainConvertor.super.doToPo(domainObject);
    }

    @Override
    public OpenOperationCaseEventDO poToDo(OpenOperationCaseEventPO po) {
        return IDomainConvertor.super.poToDo(po);
    }
}
