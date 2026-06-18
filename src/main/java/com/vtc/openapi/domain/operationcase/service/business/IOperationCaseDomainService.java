package com.vtc.openapi.domain.operationcase.service.business;

import com.botany.spore.core.page.PageInfo;
import com.vtc.openapi.domain.open.model.InvocationContext;
import com.vtc.openapi.domain.operationcase.model.entity.OpenOperationCaseDO;
import com.vtc.openapi.domain.operationcase.model.entity.OpenOperationCaseEventDO;
import com.vtc.openapi.domain.operationcase.model.query.OperationCaseAdminQuery;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.ui.dto.ApiResponse;

public interface IOperationCaseDomainService {

    /**
     * Partner 写操作受理时创建案件（ACCEPTED）。
     */
    void openAccepted(InvocationContext ctx);

    /**
     * API 调用完成时更新案件状态与摘要。
     */
    void completeOnInvocationFinish(InvocationContext ctx, ApiResponse<?> response);

    /**
     * 修复核验受理后绑定 job ↔ case。
     */
    void bindVerifyFixJob(String caseId, String jobId, String batchId);

    /**
     * VTC 复扫下发成功。
     */
    void onVerifyFixJobDispatched(OpenVerifyFixJobDO job);

    /**
     * 修复核验任务进入终态。
     */
    void onVerifyFixJobTerminal(OpenVerifyFixJobDO job);

    /**
     * OPEN 编排任务创建后与案件互指。
     */
    void bindTaskScan(String caseId, String taskId);

    /**
     * OPEN 编排任务进入终态时同步案件。
     */
    void onTaskScanTerminal(OpenTaskDO task);

    PageInfo<OpenOperationCaseDO> pageCases(OperationCaseAdminQuery query);

    OpenOperationCaseDO requireCase(String caseId);

    java.util.List<OpenOperationCaseEventDO> listEvents(String caseId);
}
