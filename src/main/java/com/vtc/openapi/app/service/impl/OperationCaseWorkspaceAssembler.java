package com.vtc.openapi.app.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.app.convert.AdminGovernanceAppConvertor;
import com.vtc.openapi.app.convert.VerifyFixJobAdminConvertor;
import com.vtc.openapi.app.service.IOpenTaskAdminAppService;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceLogDO;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceLogRepository;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.instance.service.business.IVerifyFixJobDomainService;
import com.vtc.openapi.domain.open.OpenApiOperations;
import com.vtc.openapi.domain.open.model.entity.ApiInvocationDO;
import com.vtc.openapi.domain.open.model.entity.WebhookDeliveryLogDO;
import com.vtc.openapi.domain.open.repository.IApiInvocationRepository;
import com.vtc.openapi.domain.operationcase.model.OperationCaseTypes;
import com.vtc.openapi.domain.operationcase.model.entity.OpenOperationCaseDO;
import com.vtc.openapi.domain.operationcase.model.entity.OpenOperationCaseTargetDO;
import com.vtc.openapi.domain.operationcase.repository.IOpenOperationCaseRepository;
import com.vtc.openapi.ui.dto.admin.MockVerifyFixJobDto;
import com.vtc.openapi.ui.dto.admin.OperationCaseBatchPayloadDto;
import com.vtc.openapi.ui.dto.admin.OperationCaseBatchTargetDto;
import com.vtc.openapi.ui.dto.admin.OperationCaseInstancePayloadDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskWorkspaceDto;
import com.vtc.openapi.ui.dto.admin.OperationCaseTaskScanPayloadDto;
import com.vtc.openapi.ui.dto.admin.OperationCaseVerifyFixPayloadDto;
import com.vtc.openapi.ui.dto.admin.OpenVulnInstanceStateLogDto;
import com.vtc.openapi.ui.dto.admin.WebhookDeliveryLogDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

@Component
public class OperationCaseWorkspaceAssembler {

    private static final SimpleDateFormat ISO_UTC;

    static {
        ISO_UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        ISO_UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final IOpenVulnInstanceRepository vulnInstanceRepository;
    private final IOpenVulnInstanceLogRepository vulnInstanceLogRepository;
    private final IVerifyFixJobDomainService verifyFixJobDomainService;
    private final IApiInvocationRepository apiInvocationRepository;
    private final AdminGovernanceAppConvertor adminGovernanceAppConvertor;
    private final VerifyFixJobAdminConvertor verifyFixJobAdminConvertor;
    private final IOpenTaskAdminAppService openTaskAdminAppService;
    private final IOpenOperationCaseRepository operationCaseRepository;

    public OperationCaseWorkspaceAssembler(IOpenVulnInstanceRepository vulnInstanceRepository,
                                           IOpenVulnInstanceLogRepository vulnInstanceLogRepository,
                                           IVerifyFixJobDomainService verifyFixJobDomainService,
                                           IApiInvocationRepository apiInvocationRepository,
                                           AdminGovernanceAppConvertor adminGovernanceAppConvertor,
                                           VerifyFixJobAdminConvertor verifyFixJobAdminConvertor,
                                           IOpenTaskAdminAppService openTaskAdminAppService,
                                           IOpenOperationCaseRepository operationCaseRepository) {
        this.vulnInstanceRepository = vulnInstanceRepository;
        this.vulnInstanceLogRepository = vulnInstanceLogRepository;
        this.verifyFixJobDomainService = verifyFixJobDomainService;
        this.apiInvocationRepository = apiInvocationRepository;
        this.adminGovernanceAppConvertor = adminGovernanceAppConvertor;
        this.verifyFixJobAdminConvertor = verifyFixJobAdminConvertor;
        this.openTaskAdminAppService = openTaskAdminAppService;
        this.operationCaseRepository = operationCaseRepository;
    }

    public Object buildPayload(OpenOperationCaseDO row, List<ApiInvocationDO> invocations) {
        if (row == null || !StringUtils.hasText(row.getCaseType())) {
            return null;
        }
        String caseType = row.getCaseType();
        if (OperationCaseTypes.TASK_SCAN.equals(caseType)) {
            return buildTaskScanPayload(row);
        }
        if (OperationCaseTypes.VERIFY_FIX.equals(caseType)) {
            return buildVerifyFixPayload(row);
        }
        if (OperationCaseTypes.INSTANCE_BATCH.equals(caseType)) {
            return buildBatchPayload(row);
        }
        if (OperationCaseTypes.INSTANCE_VERIFY.equals(caseType)
                || OperationCaseTypes.INSTANCE_REMEDIATE.equals(caseType)) {
            return buildInstancePayload(row, invocations);
        }
        return null;
    }

    public List<OpenVulnInstanceStateLogDto> buildStateLogs(String caseId) {
        List<OpenVulnInstanceLogDO> rows = vulnInstanceLogRepository.listByCaseId(caseId, 100);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<OpenVulnInstanceStateLogDto> list = new ArrayList<>();
        for (OpenVulnInstanceLogDO row : rows) {
            list.add(toStateLogDto(row));
        }
        return list;
    }

    public List<WebhookDeliveryLogDTO> buildWebhooks(OpenOperationCaseDO row) {
        if (row == null || !StringUtils.hasText(row.getPartnerId())) {
            return Collections.emptyList();
        }
        String resourceType = null;
        String resourceId = row.getPrimaryResourceId();
        if (OperationCaseTypes.VERIFY_FIX.equals(row.getCaseType())) {
            if (!StringUtils.hasText(resourceId)) {
                resourceId = extractVerifyFixJobId(row.getResultSummaryJson());
            }
        } else if (OperationCaseTypes.TASK_SCAN.equals(row.getCaseType())) {
            resourceType = OpenApiOperations.PRIMARY_RESOURCE_TASK;
            if (!StringUtils.hasText(resourceId)) {
                resourceId = extractTaskId(row.getResultSummaryJson());
            }
        } else if (StringUtils.hasText(resourceId)
                && (OperationCaseTypes.INSTANCE_VERIFY.equals(row.getCaseType())
                || OperationCaseTypes.INSTANCE_REMEDIATE.equals(row.getCaseType()))) {
            resourceType = OpenApiOperations.RESOURCE_TYPE_INSTANCE;
        }
        if (!StringUtils.hasText(resourceId)) {
            return Collections.emptyList();
        }
        List<WebhookDeliveryLogDO> logs = apiInvocationRepository.listByResource(
                row.getPartnerId(), resourceType, resourceId, 20);
        return adminGovernanceAppConvertor.toWebhookDeliveryLogDtoList(logs);
    }

    private OperationCaseVerifyFixPayloadDto buildVerifyFixPayload(OpenOperationCaseDO row) {
        String jobId = row.getPrimaryResourceId();
        if (!StringUtils.hasText(jobId)) {
            jobId = extractVerifyFixJobId(row.getResultSummaryJson());
        }
        if (!StringUtils.hasText(jobId)) {
            return null;
        }
        OpenVerifyFixJobDO job = verifyFixJobDomainService.requireJob(jobId);
        MockVerifyFixJobDto jobDto = verifyFixJobAdminConvertor.toJobDto(job, true);
        OperationCaseVerifyFixPayloadDto payload = new OperationCaseVerifyFixPayloadDto();
        payload.setJob(jobDto);
        payload.setCenterSubId(job.getCenterSubId());
        payload.setCenterPlanId(job.getCenterPlanId());
        payload.setSurveyId(job.getSurveyId());
        payload.setScannerType(job.getScannerType());
        payload.setInputIps(job.getInputIps());
        payload.setProgress(job.getProgress());
        return payload;
    }

    private OperationCaseTaskScanPayloadDto buildTaskScanPayload(OpenOperationCaseDO row) {
        String taskId = row.getPrimaryResourceId();
        if (!StringUtils.hasText(taskId)) {
            taskId = extractTaskId(row.getResultSummaryJson());
        }
        if (!StringUtils.hasText(taskId)) {
            return null;
        }
        OperationCaseTaskScanPayloadDto payload = new OperationCaseTaskScanPayloadDto();
        payload.setTaskId(taskId);
        try {
            com.vtc.openapi.ui.dto.ApiResponse<OpenTaskWorkspaceDto> ws = openTaskAdminAppService.getWorkspace(taskId);
            if (ws != null && ws.getData() != null) {
                payload.setTaskWorkspace(ws.getData());
            }
        } catch (Exception ignored) {
            // 任务可能尚未落库，保留 taskId 供前端跳转
        }
        return payload;
    }

    private OperationCaseInstancePayloadDto buildInstancePayload(OpenOperationCaseDO row,
                                                                 List<ApiInvocationDO> invocations) {
        String vulInfoId = row.getPrimaryResourceId();
        if (!StringUtils.hasText(vulInfoId)) {
            vulInfoId = extractVulInfoId(row.getResultSummaryJson());
        }
        OperationCaseInstancePayloadDto payload = new OperationCaseInstancePayloadDto();
        payload.setVulInfoId(vulInfoId);
        payload.setRequestSummaryJson(row.getRequestSummaryJson());
        payload.setResultSummaryJson(row.getResultSummaryJson());
        if (invocations != null && !invocations.isEmpty()) {
            payload.setOperationId(invocations.get(0).getOperationId());
        }
        if (StringUtils.hasText(vulInfoId) && StringUtils.hasText(row.getPartnerId())) {
            OpenVulnInstanceDO instance = vulnInstanceRepository.findByPartnerAndVulInfoId(
                    row.getPartnerId(), vulInfoId);
            if (instance != null) {
                payload.setVulInfoStat(instance.getVulInfoStat());
                payload.setTaskId(instance.getTaskId());
                fillInstanceSnapshotFields(payload, instance.getSnapshotJson());
            }
        }
        return payload;
    }

    private OperationCaseBatchPayloadDto buildBatchPayload(OpenOperationCaseDO row) {
        OperationCaseBatchPayloadDto payload = new OperationCaseBatchPayloadDto();
        payload.setBatchId(row.getBatchId());
        payload.setResultSummaryJson(row.getResultSummaryJson());
        List<OpenOperationCaseTargetDO> targets = operationCaseRepository.listTargetsByCaseId(row.getCaseId());
        if (targets != null && !targets.isEmpty()) {
            for (OpenOperationCaseTargetDO target : targets) {
                OperationCaseBatchTargetDto item = new OperationCaseBatchTargetDto();
                item.setTargetKey(target.getTargetKey());
                item.setTargetStatus(target.getTargetStatus());
                item.setPrevStat(target.getPrevStat());
                item.setResultStat(target.getResultStat());
                item.setPayloadJson(target.getPayloadJson());
                payload.getTargets().add(item);
                if (OpenOperationCaseTargetDO.STATUS_DONE.equals(target.getTargetStatus())) {
                    payload.getSuccessVulInfoIds().add(target.getTargetKey());
                } else if (OpenOperationCaseTargetDO.STATUS_FAILED.equals(target.getTargetStatus())) {
                    payload.getFailedVulInfoIds().add(target.getTargetKey());
                }
            }
            payload.setSuccessCount(payload.getSuccessVulInfoIds().size());
            payload.setFailedCount(payload.getFailedVulInfoIds().size());
            return payload;
        }
        if (!StringUtils.hasText(row.getResultSummaryJson())) {
            return payload;
        }
        try {
            JSONObject data = JSON.parseObject(row.getResultSummaryJson());
            JSONArray success = data.getJSONArray("success");
            JSONArray failed = data.getJSONArray("failed");
            payload.setSuccessCount(success != null ? success.size() : 0);
            payload.setFailedCount(failed != null ? failed.size() : 0);
            if (success != null) {
                for (int i = 0; i < success.size(); i++) {
                    JSONObject item = success.getJSONObject(i);
                    if (item != null && StringUtils.hasText(item.getString("vulInfoID"))) {
                        payload.getSuccessVulInfoIds().add(item.getString("vulInfoID"));
                    } else if (item != null && StringUtils.hasText(item.getString("vulInfoId"))) {
                        payload.getSuccessVulInfoIds().add(item.getString("vulInfoId"));
                    }
                }
            }
            if (failed != null) {
                for (int i = 0; i < failed.size(); i++) {
                    JSONObject item = failed.getJSONObject(i);
                    if (item != null && StringUtils.hasText(item.getString("vulInfoID"))) {
                        payload.getFailedVulInfoIds().add(item.getString("vulInfoID"));
                    } else if (item != null && StringUtils.hasText(item.getString("vulInfoId"))) {
                        payload.getFailedVulInfoIds().add(item.getString("vulInfoId"));
                    }
                }
            }
        } catch (Exception ignored) {
            // keep partial payload
        }
        return payload;
    }

    private static String extractVerifyFixJobId(String resultSummaryJson) {
        return extractField(resultSummaryJson, "verifyFixJobId");
    }

    private static String extractTaskId(String resultSummaryJson) {
        String id = extractField(resultSummaryJson, "taskId");
        if (!StringUtils.hasText(id)) {
            id = extractField(resultSummaryJson, "taskID");
        }
        return id;
    }

    private static String extractVulInfoId(String resultSummaryJson) {
        String id = extractField(resultSummaryJson, "vulInfoID");
        if (!StringUtils.hasText(id)) {
            id = extractField(resultSummaryJson, "vulInfoId");
        }
        return id;
    }

    private static void fillInstanceSnapshotFields(OperationCaseInstancePayloadDto payload, String snapshotJson) {
        if (payload == null || !StringUtils.hasText(snapshotJson)) {
            return;
        }
        try {
            JSONObject snap = JSON.parseObject(snapshotJson);
            payload.setVulName(snap.getString("vulName"));
            payload.setVulNetAddr(snap.getString("vulNetAddr"));
        } catch (Exception ignored) {
            // ignore malformed snapshot
        }
    }

    private static String extractField(String json, String field) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            JSONObject data = JSON.parseObject(json);
            String direct = data.getString(field);
            if (StringUtils.hasText(direct)) {
                return direct;
            }
            JSONArray success = data.getJSONArray("success");
            if (success != null && !success.isEmpty()) {
                JSONObject first = success.getJSONObject(0);
                if (first != null) {
                    return first.getString(field);
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private static OpenVulnInstanceStateLogDto toStateLogDto(OpenVulnInstanceLogDO row) {
        OpenVulnInstanceStateLogDto dto = new OpenVulnInstanceStateLogDto();
        dto.setId(row.getId());
        dto.setCaseId(row.getCaseId());
        dto.setVulInfoId(row.getVulInfoId());
        dto.setTaskId(row.getTaskId());
        dto.setSubId(row.getSubId());
        dto.setScanPhase(row.getScanPhase());
        dto.setPrevStat(row.getPrevStat());
        dto.setVulInfoStat(row.getVulInfoStat());
        dto.setChangeReason(row.getChangeReason());
        dto.setVerifyMergeStrategy(row.getVerifyMergeStrategy());
        dto.setScannerHitCount(row.getScannerHitCount());
        dto.setTransferTime(row.getTransferTime());
        if (row.getCreatedAt() != null) {
            dto.setCreatedAt(ISO_UTC.format(row.getCreatedAt()));
        }
        return dto;
    }
}
