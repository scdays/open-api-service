package com.vtc.openapi.domain.webhook.service.business;

import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.export.service.business.VerifyFixItem;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;

import java.util.List;
import java.util.Map;

public interface IWebhookPublishService {

    void publishTaskCompleted(OpenTaskDO task, Map<String, Object> summary);

    void publishTaskFailed(OpenTaskDO task);

    void publishExportReady(OpenTaskDO task, OpenExportDO export);

    void publishVerifyFixCompleted(String partnerId, String verifyFixJobId, String batchId,
                                   List<VerifyFixItem> items);
}
