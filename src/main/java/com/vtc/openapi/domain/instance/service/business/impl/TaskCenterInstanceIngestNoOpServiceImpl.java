package com.vtc.openapi.domain.instance.service.business.impl;

import com.vtc.openapi.domain.instance.service.business.IInstanceIngestDomainService;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * task-center 模式下实例入库由 {@link com.vtc.openapi.infra.adapter.taskcenter.TaskCenterRecycleService} 完成。
 */
@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterInstanceIngestNoOpServiceImpl implements IInstanceIngestDomainService {

    @Override
    public void tryIngestOnTaskFinished(OpenTaskDO task) {
        // ingest handled in task-center recycle phase
    }
}
