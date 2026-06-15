package com.vtc.openapi.domain.export.service.business;

import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;

import java.util.List;

public interface IExportAssemblyDomainService {

    void assembleForTaskCompleted(OpenTaskDO task);

    void assembleForVerifyScan(String partnerId, String taskId);

    void assembleForVerifyFixScan(String partnerId, String taskId, String verifyFixJobId,
                                  List<VerifyFixItem> items);
}
