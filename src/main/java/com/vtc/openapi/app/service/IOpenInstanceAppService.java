package com.vtc.openapi.app.service;

import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.open.instance.InstanceBatchOperationRequest;
import com.vtc.openapi.ui.dto.open.instance.InstanceBatchOperationResponse;
import com.vtc.openapi.ui.dto.open.instance.InstanceDetailDto;
import com.vtc.openapi.ui.dto.open.instance.InstanceOperationResponse;
import com.vtc.openapi.ui.dto.open.instance.InstanceSearchRequest;
import com.vtc.openapi.ui.dto.open.instance.InstanceSearchResponse;
import com.vtc.openapi.ui.dto.open.instance.RemediateInstanceRequest;
import com.vtc.openapi.ui.dto.open.instance.VerifyFixInstanceRequest;
import com.vtc.openapi.ui.dto.open.instance.VerifyInstanceRequest;

public interface IOpenInstanceAppService {

    ApiResponse<InstanceSearchResponse> searchInstances(InstanceSearchRequest request);

    ApiResponse<InstanceDetailDto> getInstance(String vulInfoId);

    ApiResponse<InstanceOperationResponse> verifyInstance(String vulInfoId, VerifyInstanceRequest request);

    ApiResponse<InstanceOperationResponse> remediateInstance(String vulInfoId, RemediateInstanceRequest request);

    ApiResponse<InstanceOperationResponse> verifyFixInstance(String vulInfoId, VerifyFixInstanceRequest request);

    // --- batch ---
    ApiResponse<InstanceBatchOperationResponse> verifyInstanceBatch(InstanceBatchOperationRequest request);

    ApiResponse<InstanceBatchOperationResponse> remediateInstanceBatch(InstanceBatchOperationRequest request);

    ApiResponse<InstanceBatchOperationResponse> verifyFixInstanceBatch(InstanceBatchOperationRequest request);
}
