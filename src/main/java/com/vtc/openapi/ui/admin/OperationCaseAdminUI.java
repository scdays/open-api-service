package com.vtc.openapi.ui.admin;

import com.botany.spore.ddd.ui.BaseUI;
import com.vtc.openapi.app.service.IOperationCaseAdminAppService;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.OperationCaseAdminPageDto;
import com.vtc.openapi.ui.dto.admin.OperationCaseActionResultDto;
import com.vtc.openapi.ui.dto.admin.OperationCaseBackfillResultDto;
import com.vtc.openapi.ui.dto.admin.OperationCaseWorkspaceDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/admin/operation-cases")
@Validated
@Api(tags = "OPEN 运营案件")
public class OperationCaseAdminUI extends BaseUI {

    private final IOperationCaseAdminAppService operationCaseAdminAppService;

    public OperationCaseAdminUI(IOperationCaseAdminAppService operationCaseAdminAppService) {
        this.operationCaseAdminAppService = operationCaseAdminAppService;
    }

    @ApiOperation("分页查询运营案件")
    @GetMapping
    public ApiResponse<OperationCaseAdminPageDto> listCases(
            @RequestParam(value = "partnerId", required = false) String partnerId,
            @RequestParam(value = "caseType", required = false) String caseType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "primaryResourceId", required = false) String primaryResourceId,
            @RequestParam(value = "caseId", required = false) String caseId,
            @RequestParam(value = "startedFrom", required = false) String startedFrom,
            @RequestParam(value = "startedTo", required = false) String startedTo,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return operationCaseAdminAppService.listCases(
                partnerId, caseType, status, primaryResourceId, caseId, startedFrom, startedTo, page, size);
    }

    @ApiOperation("运营案件统一工作台")
    @GetMapping("/{caseId}/workspace")
    public ApiResponse<OperationCaseWorkspaceDto> getWorkspace(@PathVariable("caseId") String caseId) {
        return operationCaseAdminAppService.getWorkspace(caseId);
    }

    @ApiOperation("历史数据回填 case_id（W4）")
    @PostMapping("/backfill")
    public ApiResponse<OperationCaseBackfillResultDto> backfill(
            @RequestParam(value = "partnerId", required = false) String partnerId,
            @RequestParam(value = "limit", defaultValue = "200") int limit,
            @RequestParam(value = "dryRun", defaultValue = "false") boolean dryRun) {
        return operationCaseAdminAppService.backfill(partnerId, limit, dryRun);
    }

    @ApiOperation("运营重试下发（TASK_SCAN / VERIFY_FIX）")
    @PostMapping("/{caseId}/actions/retry-dispatch")
    public ApiResponse<OperationCaseActionResultDto> retryDispatch(
            @PathVariable("caseId") String caseId,
            @RequestParam(value = "scanPhase", required = false) Integer scanPhase,
            @RequestParam(value = "subId", required = false) String subId) {
        return operationCaseAdminAppService.retryDispatch(caseId, scanPhase, subId);
    }
}
