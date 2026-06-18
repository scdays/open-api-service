package com.vtc.openapi.app.service;

import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.OperationCaseActionResultDto;
import com.vtc.openapi.ui.dto.admin.OperationCaseAdminPageDto;
import com.vtc.openapi.ui.dto.admin.OperationCaseBackfillResultDto;
import com.vtc.openapi.ui.dto.admin.OperationCaseWorkspaceDto;

public interface IOperationCaseAdminAppService {

    ApiResponse<OperationCaseAdminPageDto> listCases(String partnerId, String caseType, String status,
                                                     String primaryResourceId, String caseId,
                                                     String startedFrom, String startedTo,
                                                     int page, int size);

    ApiResponse<OperationCaseWorkspaceDto> getWorkspace(String caseId);

    ApiResponse<OperationCaseBackfillResultDto> backfill(String partnerId, int limit, boolean dryRun);

    ApiResponse<OperationCaseActionResultDto> retryDispatch(String caseId, Integer scanPhase, String subId);
}
