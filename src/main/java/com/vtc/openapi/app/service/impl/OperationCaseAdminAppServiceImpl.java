package com.vtc.openapi.app.service.impl;

import com.botany.spore.core.page.PageInfo;
import com.vtc.openapi.app.convert.AdminGovernanceAppConvertor;
import com.vtc.openapi.app.service.IOpenTaskAdminAppService;
import com.vtc.openapi.app.service.IOperationCaseAdminAppService;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.open.model.entity.ApiInvocationDO;
import com.vtc.openapi.domain.open.repository.IApiInvocationRepository;
import com.vtc.openapi.domain.operationcase.model.OperationCaseTypes;
import com.vtc.openapi.domain.operationcase.model.entity.OpenOperationCaseDO;
import com.vtc.openapi.domain.operationcase.model.entity.OpenOperationCaseEventDO;
import com.vtc.openapi.domain.operationcase.model.query.OperationCaseAdminQuery;
import com.vtc.openapi.domain.operationcase.service.business.IOperationCaseDomainService;
import com.vtc.openapi.domain.operationcase.service.business.impl.OperationCaseBackfillService;
import com.vtc.openapi.infra.adapter.taskcenter.TaskCenterVerifyFixOrchestrator;
import com.vtc.openapi.infra.config.OpenApiProperties;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.OperationCaseActionResultDto;
import com.vtc.openapi.ui.dto.admin.OperationCaseAdminDto;
import com.vtc.openapi.ui.dto.admin.OperationCaseAdminPageDto;
import com.vtc.openapi.ui.dto.admin.OperationCaseBackfillResultDto;
import com.vtc.openapi.ui.dto.admin.OperationCaseEventDto;
import com.vtc.openapi.ui.dto.admin.OperationCaseWorkspaceDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskDispatchRetryResultDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OperationCaseAdminAppServiceImpl implements IOperationCaseAdminAppService {

    private static final SimpleDateFormat SIMPLE_DATETIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final IOperationCaseDomainService operationCaseDomainService;
    private final IApiInvocationRepository apiInvocationRepository;
    private final AdminGovernanceAppConvertor adminGovernanceAppConvertor;
    private final OperationCaseWorkspaceAssembler workspaceAssembler;
    private final OperationCaseBackfillService operationCaseBackfillService;
    private final IOpenTaskAdminAppService openTaskAdminAppService;
    private final OpenApiProperties openApiProperties;

    @Autowired(required = false)
    private TaskCenterVerifyFixOrchestrator taskCenterVerifyFixOrchestrator;

    public OperationCaseAdminAppServiceImpl(IOperationCaseDomainService operationCaseDomainService,
                                            IApiInvocationRepository apiInvocationRepository,
                                            AdminGovernanceAppConvertor adminGovernanceAppConvertor,
                                            OperationCaseWorkspaceAssembler workspaceAssembler,
                                            OperationCaseBackfillService operationCaseBackfillService,
                                            IOpenTaskAdminAppService openTaskAdminAppService,
                                            OpenApiProperties openApiProperties) {
        this.operationCaseDomainService = operationCaseDomainService;
        this.apiInvocationRepository = apiInvocationRepository;
        this.adminGovernanceAppConvertor = adminGovernanceAppConvertor;
        this.workspaceAssembler = workspaceAssembler;
        this.operationCaseBackfillService = operationCaseBackfillService;
        this.openTaskAdminAppService = openTaskAdminAppService;
        this.openApiProperties = openApiProperties;
    }

    @Override
    public ApiResponse<OperationCaseAdminPageDto> listCases(String partnerId, String caseType, String status,
                                                            String primaryResourceId, String caseId,
                                                            String startedFrom, String startedTo,
                                                            int page, int size) {
        OperationCaseAdminQuery query = new OperationCaseAdminQuery();
        query.setPartnerId(partnerId);
        query.setCaseType(caseType);
        query.setStatus(status);
        query.setPrimaryResourceId(primaryResourceId);
        query.setCaseId(caseId);
        query.setStartedFrom(parseDateTime(startedFrom));
        query.setStartedTo(parseDateTime(startedTo));
        query.setPage(page);
        query.setSize(size);

        PageInfo<OpenOperationCaseDO> pageInfo = operationCaseDomainService.pageCases(query);
        OperationCaseAdminPageDto dto = new OperationCaseAdminPageDto();
        dto.setPage((int) pageInfo.getCurrent());
        dto.setSize((int) pageInfo.getSize());
        dto.setTotal(pageInfo.getTotal());
        if (CollectionUtils.isEmpty(pageInfo.getRecords())) {
            dto.setItems(Collections.emptyList());
        } else {
            dto.setItems(pageInfo.getRecords().stream()
                    .map(this::toAdminDto)
                    .collect(Collectors.toList()));
        }
        return ApiResponse.ok(dto);
    }

    @Override
    public ApiResponse<OperationCaseWorkspaceDto> getWorkspace(String caseId) {
        OpenOperationCaseDO row = operationCaseDomainService.requireCase(caseId);
        List<OpenOperationCaseEventDO> events = operationCaseDomainService.listEvents(caseId);
        List<ApiInvocationDO> invocations = apiInvocationRepository.listByCaseId(caseId, 50);

        OperationCaseWorkspaceDto workspace = new OperationCaseWorkspaceDto();
        workspace.setCaseSummary(toAdminDto(row));
        workspace.setTimeline(events.stream().map(this::toEventDto).collect(Collectors.toList()));
        workspace.setInvocations(adminGovernanceAppConvertor.toInvocationDtoList(invocations));
        workspace.setStateLogs(workspaceAssembler.buildStateLogs(caseId));
        workspace.setWebhooks(workspaceAssembler.buildWebhooks(row));
        workspace.setPayload(workspaceAssembler.buildPayload(row, invocations));
        return ApiResponse.ok(workspace);
    }

    @Override
    public ApiResponse<OperationCaseBackfillResultDto> backfill(String partnerId, int limit, boolean dryRun) {
        return ApiResponse.ok(operationCaseBackfillService.backfill(partnerId, limit, dryRun));
    }

    @Override
    public ApiResponse<OperationCaseActionResultDto> retryDispatch(String caseId, Integer scanPhase, String subId) {
        OpenOperationCaseDO row = operationCaseDomainService.requireCase(caseId);
        OperationCaseActionResultDto dto = new OperationCaseActionResultDto();
        dto.setCaseId(caseId);
        dto.setActionType("RETRY_DISPATCH");

        if (OperationCaseTypes.TASK_SCAN.equals(row.getCaseType())) {
            String taskId = row.getPrimaryResourceId();
            if (!StringUtils.hasText(taskId)) {
                throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "案件未绑定 taskId");
            }
            ApiResponse<OpenTaskDispatchRetryResultDto> retry = openTaskAdminAppService.retrySurveyDispatch(
                    taskId, scanPhase, subId);
            boolean ok = retry != null && retry.getData() != null && retry.getData().getSuccess();
            dto.setSuccess(ok);
            dto.setMessage(retry != null && retry.getData() != null ? retry.getData().getMessage() : "重试已提交");
            return ApiResponse.ok(dto);
        }

        if (OperationCaseTypes.VERIFY_FIX.equals(row.getCaseType())) {
            if (!"task-center".equalsIgnoreCase(openApiProperties.getEngine().getAdapterMode())) {
                throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID, "仅 task-center 模式支持修复核验重试下发");
            }
            if (taskCenterVerifyFixOrchestrator == null) {
                throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID, "task-center 编排未启用");
            }
            String jobId = row.getPrimaryResourceId();
            if (!StringUtils.hasText(jobId)) {
                throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "案件未绑定 verifyFixJobId");
            }
            boolean ok = taskCenterVerifyFixOrchestrator.retryDispatchForJob(jobId);
            dto.setSuccess(ok);
            dto.setMessage(ok ? "已触发 VTC 复扫下发" : "当前 job 状态不可重试下发");
            return ApiResponse.ok(dto);
        }

        throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID, "当前案件类型不支持重试下发");
    }

    private OperationCaseAdminDto toAdminDto(OpenOperationCaseDO row) {
        OperationCaseAdminDto dto = new OperationCaseAdminDto();
        dto.setCaseId(row.getCaseId());
        dto.setPartnerId(row.getPartnerId());
        dto.setCaseType(row.getCaseType());
        dto.setStatus(row.getStatus());
        dto.setTitle(row.getTitle());
        dto.setPrimaryResourceType(row.getPrimaryResourceType());
        dto.setPrimaryResourceId(row.getPrimaryResourceId());
        dto.setBatchId(row.getBatchId());
        dto.setInvocationId(row.getInvocationId());
        dto.setErrorMessage(row.getErrorMessage());
        dto.setStartedAt(row.getStartedAt());
        dto.setFinishedAt(row.getFinishedAt());
        return dto;
    }

    private OperationCaseEventDto toEventDto(OpenOperationCaseEventDO row) {
        OperationCaseEventDto dto = new OperationCaseEventDto();
        dto.setId(row.getId());
        dto.setEventType(row.getEventType());
        dto.setEventPayloadJson(row.getEventPayloadJson());
        dto.setCreatedAt(row.getCreatedAt());
        return dto;
    }

    private Date parseDateTime(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return SIMPLE_DATETIME.parse(text.trim());
        } catch (ParseException ex) {
            return null;
        }
    }
}
