package com.vtc.openapi.domain.operationcase.model.convert;

import com.botany.spore.ddd.domain.model.convertor.IDomainConvertor;
import com.vtc.openapi.domain.operationcase.model.entity.OpenOperationCaseTargetDO;
import com.vtc.openapi.infra.dao.po.OpenOperationCaseTargetPO;
import org.springframework.stereotype.Component;

@Component
public class OpenOperationCaseTargetDomainConvertor
        implements IDomainConvertor<OpenOperationCaseTargetDO, OpenOperationCaseTargetPO> {

    @Override
    public OpenOperationCaseTargetPO doToPo(OpenOperationCaseTargetDO domainObject) {
        return IDomainConvertor.super.doToPo(domainObject);
    }

    @Override
    public OpenOperationCaseTargetDO poToDo(OpenOperationCaseTargetPO po) {
        return IDomainConvertor.super.poToDo(po);
    }
}
