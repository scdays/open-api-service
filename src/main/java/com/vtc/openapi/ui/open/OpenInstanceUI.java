package com.vtc.openapi.ui.open;

import com.vtc.openapi.app.service.IOpenInstanceAppService;
import com.vtc.openapi.domain.open.OpenApiConstants;
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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开放平台实例 REST（/api/open/v1/instances）。P1 OP-OPENAPI-P1。
 */
@RestController
@RequestMapping(OpenApiConstants.API_PREFIX)
@Api(tags = "开放平台 · 实例")
public class OpenInstanceUI {

    private final IOpenInstanceAppService openInstanceAppService;

    public OpenInstanceUI(IOpenInstanceAppService openInstanceAppService) {
        this.openInstanceAppService = openInstanceAppService;
    }

    // ===================== 读接口（无幂等） =====================

    @ApiOperation(value = "搜索实例", notes = "POST /instances/search - INSTANCE_READ；查询参数 exportProfile 可选")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Partner-Id", value = "Partner ID", required = true,
                    paramType = "header", dataType = "string"),
            @ApiImplicitParam(name = "X-Request-Id", value = "请求追踪 ID", paramType = "header", dataType = "string"),
            @ApiImplicitParam(name = "exportProfile", value = "部侧扩展字段档，如 MIIT-2025",
                    paramType = "query", dataType = "string")
    })
    @PostMapping("/instances/search")
    public ApiResponse<InstanceSearchResponse> searchInstances(
            @RequestParam(value = "exportProfile", required = false) String exportProfile,
            @RequestBody InstanceSearchRequest request) {
        return openInstanceAppService.searchInstances(request, exportProfile);
    }

    @ApiOperation(value = "查询实例详情", notes = "GET /instances/{vulInfoID} - INSTANCE_READ")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Partner-Id", value = "Partner ID", required = true,
                    paramType = "header", dataType = "string"),
            @ApiImplicitParam(name = "X-Request-Id", value = "请求追踪 ID", paramType = "header", dataType = "string")
    })
    @GetMapping("/instances/{vulInfoID}")
    public ApiResponse<InstanceDetailDto> getInstance(@PathVariable("vulInfoID") String vulInfoId) {
        return openInstanceAppService.getInstance(vulInfoId);
    }

    // ===================== 写接口（单条，支持 Idempotency-Key） =====================

    @ApiOperation(value = "验证实例", notes = "POST /instances/{vulInfoID}/verify - INSTANCE_VERIFY · 幂等见 §4.2")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Partner-Id", value = "Partner ID", required = true,
                    paramType = "header", dataType = "string"),
            @ApiImplicitParam(name = "X-Request-Id", value = "请求追踪 ID", paramType = "header", dataType = "string"),
            @ApiImplicitParam(name = "Idempotency-Key", value = "写操作幂等键（推荐 verify:{vulInfoID}:{clientReqId}）",
                    paramType = "header", dataType = "string")
    })
    @PostMapping("/instances/{vulInfoID}/verify")
    public ApiResponse<InstanceOperationResponse> verifyInstance(
            @PathVariable("vulInfoID") String vulInfoId,
            @RequestBody VerifyInstanceRequest request) {
        return openInstanceAppService.verifyInstance(vulInfoId, request);
    }

    @ApiOperation(value = "修复实例", notes = "POST /instances/{vulInfoID}/remediate - INSTANCE_REMEDIATE · 幂等见 §4.2")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Partner-Id", value = "Partner ID", required = true,
                    paramType = "header", dataType = "string"),
            @ApiImplicitParam(name = "X-Request-Id", value = "请求追踪 ID", paramType = "header", dataType = "string"),
            @ApiImplicitParam(name = "Idempotency-Key", value = "写操作幂等键（推荐 remediate:{vulInfoID}:{clientReqId}）",
                    paramType = "header", dataType = "string")
    })
    @PostMapping("/instances/{vulInfoID}/remediate")
    public ApiResponse<InstanceOperationResponse> remediateInstance(
            @PathVariable("vulInfoID") String vulInfoId,
            @RequestBody(required = false) RemediateInstanceRequest request) {
        return openInstanceAppService.remediateInstance(vulInfoId, request);
    }

    @ApiOperation(value = "核验修复", notes = "POST /instances/{vulInfoID}/verify-fix - INSTANCE_VERIFY_FIX · 幂等见 §4.2")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Partner-Id", value = "Partner ID", required = true,
                    paramType = "header", dataType = "string"),
            @ApiImplicitParam(name = "X-Request-Id", value = "请求追踪 ID", paramType = "header", dataType = "string"),
            @ApiImplicitParam(name = "Idempotency-Key", value = "写操作幂等键（推荐 verify-fix:{vulInfoID}:{clientReqId}）",
                    paramType = "header", dataType = "string")
    })
    @PostMapping("/instances/{vulInfoID}/verify-fix")
    public ApiResponse<InstanceOperationResponse> verifyFixInstance(
            @PathVariable("vulInfoID") String vulInfoId,
            @RequestBody VerifyFixInstanceRequest request) {
        return openInstanceAppService.verifyFixInstance(vulInfoId, request);
    }

    // ===================== 写接口（批量，Idempotency-Key 作用于整批） =====================

    @ApiOperation(value = "批量验证实例", notes = "POST /instances/verify:batch - INSTANCE_VERIFY · 幂等见 §4.2")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Partner-Id", value = "Partner ID", required = true,
                    paramType = "header", dataType = "string"),
            @ApiImplicitParam(name = "X-Request-Id", value = "请求追踪 ID", paramType = "header", dataType = "string"),
            @ApiImplicitParam(name = "Idempotency-Key", value = "整批幂等键（推荐 verify:batch:{clientBatchId}）",
                    paramType = "header", dataType = "string")
    })
    @PostMapping("/instances/verify:batch")
    public ApiResponse<InstanceBatchOperationResponse> verifyInstanceBatch(
            @RequestBody InstanceBatchOperationRequest request) {
        return openInstanceAppService.verifyInstanceBatch(request);
    }

    @ApiOperation(value = "批量修复实例", notes = "POST /instances/remediate:batch - INSTANCE_REMEDIATE · 幂等见 §4.2")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Partner-Id", value = "Partner ID", required = true,
                    paramType = "header", dataType = "string"),
            @ApiImplicitParam(name = "X-Request-Id", value = "请求追踪 ID", paramType = "header", dataType = "string"),
            @ApiImplicitParam(name = "Idempotency-Key", value = "整批幂等键（推荐 remediate:batch:{clientBatchId}）",
                    paramType = "header", dataType = "string")
    })
    @PostMapping("/instances/remediate:batch")
    public ApiResponse<InstanceBatchOperationResponse> remediateInstanceBatch(
            @RequestBody InstanceBatchOperationRequest request) {
        return openInstanceAppService.remediateInstanceBatch(request);
    }

    @ApiOperation(value = "批量核验修复", notes = "POST /instances/verify-fix:batch - INSTANCE_VERIFY_FIX · 幂等见 §4.2")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Partner-Id", value = "Partner ID", required = true,
                    paramType = "header", dataType = "string"),
            @ApiImplicitParam(name = "X-Request-Id", value = "请求追踪 ID", paramType = "header", dataType = "string"),
            @ApiImplicitParam(name = "Idempotency-Key", value = "整批幂等键（推荐 verify-fix:batch:{clientBatchId}）",
                    paramType = "header", dataType = "string")
    })
    @PostMapping("/instances/verify-fix:batch")
    public ApiResponse<InstanceBatchOperationResponse> verifyFixInstanceBatch(
            @RequestBody InstanceBatchOperationRequest request) {
        return openInstanceAppService.verifyFixInstanceBatch(request);
    }
}