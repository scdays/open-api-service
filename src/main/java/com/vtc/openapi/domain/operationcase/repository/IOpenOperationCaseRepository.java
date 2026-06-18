package com.vtc.openapi.domain.operationcase.repository;

import com.botany.spore.core.page.PageInfo;
import com.vtc.openapi.domain.operationcase.model.entity.OpenOperationCaseDO;
import com.vtc.openapi.domain.operationcase.model.entity.OpenOperationCaseEventDO;
import com.vtc.openapi.domain.operationcase.model.entity.OpenOperationCaseTargetDO;
import com.vtc.openapi.domain.operationcase.model.query.OperationCaseAdminQuery;

import java.util.List;

public interface IOpenOperationCaseRepository {

    void insert(OpenOperationCaseDO row);

    void updateOnFinish(OpenOperationCaseDO patch);

    void updateProgress(OpenOperationCaseDO patch);

    OpenOperationCaseDO findByCaseId(String caseId);

    OpenOperationCaseDO findByInvocationId(String invocationId);

    OpenOperationCaseDO findByPrimaryResource(String partnerId, String primaryResourceType, String primaryResourceId);

    PageInfo<OpenOperationCaseDO> pageCases(OperationCaseAdminQuery query);

    List<OpenOperationCaseDO> listRecent(String partnerId, int limit);

    void insertEvent(OpenOperationCaseEventDO event);

    List<OpenOperationCaseEventDO> listEventsByCaseId(String caseId);

    void insertTargets(List<OpenOperationCaseTargetDO> targets);

    List<OpenOperationCaseTargetDO> listTargetsByCaseId(String caseId);
}
