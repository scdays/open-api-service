package com.vtc.openapi.domain.task.repository;

import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.domain.repository.IDatabaseRepository;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.entity.PartnerTaskMapDO;
import com.vtc.openapi.domain.task.model.query.OpenTaskListQuery;

public interface IOpenTaskRepository extends IDatabaseRepository<OpenTaskDO> {

    OpenTaskDO findByTaskId(String taskId);

    PartnerTaskMapDO findTaskMap(String partnerId, String extTaskId);

    void saveTaskMap(PartnerTaskMapDO map);

    PageInfo<OpenTaskDO> pageByPartner(String partnerId, OpenTaskListQuery query);

    OpenTaskDO findByEngineTaskId(String engineTaskId);
}
