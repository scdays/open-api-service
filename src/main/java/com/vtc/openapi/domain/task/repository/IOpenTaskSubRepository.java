package com.vtc.openapi.domain.task.repository;

import com.botany.spore.ddd.domain.repository.IDatabaseRepository;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;

import java.util.List;

public interface IOpenTaskSubRepository extends IDatabaseRepository<OpenTaskSubDO> {

    List<OpenTaskSubDO> listByTaskId(String taskId);

    List<OpenTaskSubDO> listByTaskIdAndPhase(String taskId, int scanPhase);

    List<OpenTaskSubDO> listRunning();

    OpenTaskSubDO findBySubId(String subId);

    void saveSub(OpenTaskSubDO row);

    void updateSub(OpenTaskSubDO row);
}
