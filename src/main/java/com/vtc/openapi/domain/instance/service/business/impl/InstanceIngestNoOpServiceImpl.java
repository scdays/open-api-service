package com.vtc.openapi.domain.instance.service.business.impl;

import com.vtc.openapi.domain.instance.service.business.IInstanceIngestDomainService;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * vul-pass 模式下不执行 Mock ingest。
 */
@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "vul-pass", matchIfMissing = true)
public class InstanceIngestNoOpServiceImpl implements IInstanceIngestDomainService {

    @Override
    public void tryIngestOnTaskFinished(OpenTaskDO task) {
        // no-op
    }
}
