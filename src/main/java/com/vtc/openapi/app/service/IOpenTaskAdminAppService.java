package com.vtc.openapi.app.service;

import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.OpenTaskDispatchRetryResultDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskAdminPageDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskReportRefetchResultDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskSurveyRefetchResultDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskSurveyResultsDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskWorkspaceDto;

public interface IOpenTaskAdminAppService {

    ApiResponse<OpenTaskAdminPageDto> listTasks(String partnerId, String taskId, String extTaskId,
                                                String status, Integer scanTemplateId, Integer vulnType,
                                                int page, int size);

    ApiResponse<OpenTaskWorkspaceDto> getWorkspace(String taskId);

    ApiResponse<OpenTaskSurveyResultsDto> getSurveyResults(String taskId, Integer scanPhase, String subId);

    ApiResponse<OpenTaskDispatchRetryResultDto> retrySurveyDispatch(String taskId, Integer scanPhase, String subId);

    ApiResponse<OpenTaskSurveyRefetchResultDto> refetchSurveyResults(String taskId, String subId);

    ApiResponse<OpenTaskReportRefetchResultDto> refetchSubReport(String taskId, String subId);

    ApiResponse<OpenTaskReportRefetchResultDto> refetchAllReports(String taskId);
}
