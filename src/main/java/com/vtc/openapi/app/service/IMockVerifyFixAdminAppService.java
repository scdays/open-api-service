package com.vtc.openapi.app.service;

import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.MockVerifyFixCompleteResultDto;
import com.vtc.openapi.ui.dto.admin.MockVerifyFixJobDto;
import com.vtc.openapi.ui.dto.admin.OfflineTaskVerifyFixContextDto;
import com.vtc.openapi.ui.dto.admin.VerifyFixInvocationCandidateDto;
import com.vtc.openapi.ui.params.admin.CreateInternalVerifyFixJobParams;
import com.vtc.openapi.ui.params.admin.CreateVerifyFixJobFromSelectionParams;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IMockVerifyFixAdminAppService {

    ApiResponse<List<MockVerifyFixJobDto>> listJobs(String partnerId, String status, int limit);

    ApiResponse<MockVerifyFixJobDto> getJob(String jobId);

    ApiResponse<MockVerifyFixCompleteResultDto> importRescanXml(String jobId, MultipartFile file);

    ApiResponse<MockVerifyFixCompleteResultDto> completeAllFixed(String jobId);

    ApiResponse<MockVerifyFixCompleteResultDto> completeAllUnfixed(String jobId);

    ApiResponse<MockVerifyFixCompleteResultDto> completeByCompare(String jobId);

    ApiResponse<OfflineTaskVerifyFixContextDto> getOfflineTaskContext(String partnerId, String taskId);

    ApiResponse<MockVerifyFixJobDto> createFromOfflineTask(CreateInternalVerifyFixJobParams params);

    ApiResponse<List<VerifyFixInvocationCandidateDto>> listInvocationCandidates(String partnerId, int limit);

    ApiResponse<MockVerifyFixJobDto> createFromSelection(CreateVerifyFixJobFromSelectionParams params);
}
