package com.vtc.openapi.domain.task.repository;

import com.vtc.openapi.domain.task.model.entity.OpenTaskScanResultDO;

import java.util.List;

public interface IOpenTaskScanResultRepository {

    void upsertBatch(List<OpenTaskScanResultDO> rows);

    List<OpenTaskScanResultDO> listByTaskAndType(String taskId, int scanPhase, String resultType);

    List<OpenTaskScanResultDO> listBySubId(String subId, String resultType);
}
