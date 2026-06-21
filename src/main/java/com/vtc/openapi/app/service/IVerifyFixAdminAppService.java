package com.vtc.openapi.app.service;

import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.MockVerifyFixJobDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskSurveyRefetchResultDto;
import com.vtc.openapi.ui.dto.admin.VerifyFixPendingInstanceDto;
import com.vtc.openapi.ui.dto.admin.VerifyFixWorkspaceDto;

import java.util.List;

public interface IVerifyFixAdminAppService {

    ApiResponse<List<MockVerifyFixJobDto>> listJobs(String partnerId, String status, String taskId,
                                                     String jobId, int limit);

    ApiResponse<VerifyFixWorkspaceDto> getWorkspace(String jobId);

    ApiResponse<List<VerifyFixPendingInstanceDto>> listPendingInstances(String partnerId, String taskId,
                                                                        String jobId, int limit);

    ApiResponse<OpenTaskSurveyRefetchResultDto> refetchRescanSub(String jobId, String subId);

    ApiResponse<Boolean> retryDispatch(String jobId);

    ApiResponse<Boolean> retryDispatchSub(String jobId, String subId);
}
