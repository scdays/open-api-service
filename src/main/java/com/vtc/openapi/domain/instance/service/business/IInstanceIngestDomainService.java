package com.vtc.openapi.domain.instance.service.business;

import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;

/**
 * Mock 模式下任务完成后将 fixture 实例写入 open_vuln_instance。
 */
public interface IInstanceIngestDomainService {

    /**
     * 任务首次进入 FINISHED 时尝试 ingest（幂等）。
     */
    void tryIngestOnTaskFinished(OpenTaskDO task);
}
