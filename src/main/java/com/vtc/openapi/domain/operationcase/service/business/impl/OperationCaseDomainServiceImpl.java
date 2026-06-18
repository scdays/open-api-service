package com.vtc.openapi.domain.operationcase.service.business.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.botany.spore.core.page.PageInfo;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.open.OpenApiOperations;
import com.vtc.openapi.domain.open.model.InvocationContext;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobDO;
import com.vtc.openapi.domain.instance.service.business.IVerifyFixJobDomainService;
import com.vtc.openapi.domain.operationcase.model.OperationCaseEventTypes;
import com.vtc.openapi.domain.operationcase.model.OperationCaseStatuses;
import com.vtc.openapi.domain.operationcase.model.OperationCaseTypes;
import com.vtc.openapi.domain.operationcase.model.entity.OpenOperationCaseDO;
import com.vtc.openapi.domain.operationcase.model.entity.OpenOperationCaseEventDO;
import com.vtc.openapi.domain.operationcase.model.entity.OpenOperationCaseTargetDO;
import com.vtc.openapi.domain.operationcase.model.query.OperationCaseAdminQuery;
import com.vtc.openapi.domain.operationcase.model.support.OperationCaseSupport;
import com.vtc.openapi.domain.operationcase.repository.IOpenOperationCaseRepository;
import com.vtc.openapi.domain.operationcase.service.business.IOperationCaseDomainService;
import com.vtc.openapi.domain.open.repository.IApiInvocationRepository;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.ui.dto.ApiResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class OperationCaseDomainServiceImpl implements IOperationCaseDomainService {

    private static final int SUMMARY_MAX = 2048;

    private final IOpenOperationCaseRepository operationCaseRepository;
    private final IApiInvocationRepository apiInvocationRepository;
    private final IOpenTaskRepository openTaskRepository;

    public OperationCaseDomainServiceImpl(IOpenOperationCaseRepository operationCaseRepository,
                                          IApiInvocationRepository apiInvocationRepository,
                                          IOpenTaskRepository openTaskRepository) {
        this.operationCaseRepository = operationCaseRepository;
        this.apiInvocationRepository = apiInvocationRepository;
        this.openTaskRepository = openTaskRepository;
    }

    @Override
    public void openAccepted(InvocationContext ctx) {
        if (ctx == null || !OperationCaseSupport.isCaseOperation(ctx.getOperationId())) {
            return;
        }
        String caseType = OperationCaseSupport.resolveCaseType(ctx.getOperationId());
        String caseId = "CASE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Date now = new Date();

        OpenOperationCaseDO row = new OpenOperationCaseDO();
        row.setCaseId(caseId);
        row.setPartnerId(ctx.getPartnerId());
        row.setCaseType(caseType);
        row.setStatus(OperationCaseStatuses.ACCEPTED);
        row.setTitle(OperationCaseSupport.buildTitle(caseType, ctx.getOperationId()));
        row.setInvocationId(ctx.getInvocationId());
        row.setIdempotencyKey(ctx.getIdempotencyKey());
        row.setRequestSummaryJson(OperationCaseSupport.truncate(ctx.getRequestBodyJson(), SUMMARY_MAX));
        row.setStartedAt(now);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        operationCaseRepository.insert(row);

        OpenOperationCaseEventDO event = new OpenOperationCaseEventDO();
        event.setCaseId(caseId);
        event.setEventType(OperationCaseEventTypes.ACCEPTED);
        JSONObject payload = new JSONObject();
        payload.put("operationId", ctx.getOperationId());
        payload.put("invocationId", ctx.getInvocationId());
        event.setEventPayloadJson(payload.toJSONString());
        event.setCreatedAt(now);
        operationCaseRepository.insertEvent(event);

        ctx.setCaseId(caseId);
        apiInvocationRepository.updateCaseId(ctx.getInvocationId(), caseId);
    }

    @Override
    public void completeOnInvocationFinish(InvocationContext ctx, ApiResponse<?> response) {
        if (ctx == null || !StringUtils.hasText(ctx.getCaseId())) {
            return;
        }
        OpenOperationCaseDO existing = operationCaseRepository.findByCaseId(ctx.getCaseId());
        String caseType = existing != null ? existing.getCaseType() : null;
        Date now = new Date();
        boolean success = response != null && response.getCode() == OpenApiConstants.CODE_OK;
        PrimaryResource primary = resolvePrimaryResource(ctx, response);
        String status = resolveTerminalStatus(success, primary, response, caseType);

        OpenOperationCaseDO patch = new OpenOperationCaseDO();
        patch.setCaseId(ctx.getCaseId());
        patch.setStatus(status);
        patch.setPrimaryResourceType(primary.type);
        patch.setPrimaryResourceId(primary.id);
        patch.setBatchId(primary.batchId);
        if (response != null && response.getData() != null) {
            patch.setResultSummaryJson(OperationCaseSupport.truncate(JSON.toJSONString(response.getData()), SUMMARY_MAX));
        }
        if (!success && response != null && StringUtils.hasText(response.getMessage())) {
            patch.setErrorMessage(OperationCaseSupport.truncate(response.getMessage(), 512));
        }
        if (OperationCaseStatuses.FINISHED.equals(status) || OperationCaseStatuses.FAILED.equals(status)) {
            patch.setFinishedAt(now);
        }
        patch.setUpdatedAt(now);
        operationCaseRepository.updateOnFinish(patch);

        if (OperationCaseTypes.INSTANCE_BATCH.equals(caseType) && StringUtils.hasText(patch.getResultSummaryJson())) {
            saveBatchTargets(ctx.getCaseId(), patch.getResultSummaryJson(), now);
        }

        OpenOperationCaseEventDO event = new OpenOperationCaseEventDO();
        event.setCaseId(ctx.getCaseId());
        if (OperationCaseStatuses.RUNNING.equals(status)) {
            event.setEventType(OperationCaseEventTypes.RUNNING);
        } else if (success) {
            event.setEventType(OperationCaseEventTypes.FINISHED);
        } else {
            event.setEventType(OperationCaseEventTypes.FAILED);
        }
        JSONObject payload = new JSONObject();
        payload.put("responseCode", response != null ? response.getCode() : null);
        payload.put("status", status);
        if (StringUtils.hasText(primary.id)) {
            payload.put("primaryResourceType", primary.type);
            payload.put("primaryResourceId", primary.id);
        }
        event.setEventPayloadJson(payload.toJSONString());
        event.setCreatedAt(now);
        operationCaseRepository.insertEvent(event);

        ctx.setResourceType(OpenApiOperations.RESOURCE_TYPE_CASE);
        ctx.setResourceId(ctx.getCaseId());
    }

    @Override
    public PageInfo<OpenOperationCaseDO> pageCases(OperationCaseAdminQuery query) {
        return operationCaseRepository.pageCases(query);
    }

    @Override
    public OpenOperationCaseDO requireCase(String caseId) {
        OpenOperationCaseDO found = operationCaseRepository.findByCaseId(caseId);
        if (found == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "运营案件不存在");
        }
        return found;
    }

    @Override
    public List<OpenOperationCaseEventDO> listEvents(String caseId) {
        return operationCaseRepository.listEventsByCaseId(caseId);
    }

    @Override
    public void bindVerifyFixJob(String caseId, String jobId, String batchId) {
        if (!StringUtils.hasText(caseId) || !StringUtils.hasText(jobId)) {
            return;
        }
        Date now = new Date();
        OpenOperationCaseDO patch = new OpenOperationCaseDO();
        patch.setCaseId(caseId);
        patch.setPrimaryResourceType(OpenApiOperations.PRIMARY_RESOURCE_VERIFY_FIX_JOB);
        patch.setPrimaryResourceId(jobId);
        if (StringUtils.hasText(batchId)) {
            patch.setBatchId(batchId);
        }
        patch.setStatus(OperationCaseStatuses.RUNNING);
        patch.setUpdatedAt(now);
        operationCaseRepository.updateProgress(patch);
    }

    @Override
    public void onVerifyFixJobDispatched(OpenVerifyFixJobDO job) {
        if (job == null || !StringUtils.hasText(job.getCaseId())) {
            return;
        }
        Date now = new Date();
        OpenOperationCaseDO patch = new OpenOperationCaseDO();
        patch.setCaseId(job.getCaseId());
        patch.setStatus(OperationCaseStatuses.RUNNING);
        patch.setUpdatedAt(now);
        operationCaseRepository.updateProgress(patch);

        JSONObject payload = new JSONObject();
        payload.put("jobId", job.getJobId());
        payload.put("centerSubId", job.getCenterSubId());
        payload.put("centerPlanId", job.getCenterPlanId());
        payload.put("surveyId", job.getSurveyId());
        insertCaseEvent(job.getCaseId(), OperationCaseEventTypes.DISPATCHED, payload, now);
    }

    @Override
    public void onVerifyFixJobTerminal(OpenVerifyFixJobDO job) {
        if (job == null || !StringUtils.hasText(job.getCaseId())) {
            return;
        }
        String status = terminalStatusForJob(job.getStatus());
        if (status == null) {
            return;
        }
        Date now = new Date();
        OpenOperationCaseDO patch = new OpenOperationCaseDO();
        patch.setCaseId(job.getCaseId());
        patch.setStatus(status);
        patch.setPrimaryResourceType(OpenApiOperations.PRIMARY_RESOURCE_VERIFY_FIX_JOB);
        patch.setPrimaryResourceId(job.getJobId());
        patch.setErrorMessage(job.getErrorMessage());
        patch.setFinishedAt(now);
        patch.setUpdatedAt(now);
        operationCaseRepository.updateProgress(patch);

        JSONObject payload = new JSONObject();
        payload.put("jobId", job.getJobId());
        payload.put("jobStatus", job.getStatus());
        payload.put("caseStatus", status);
        String eventType = OperationCaseStatuses.FINISHED.equals(status)
                ? OperationCaseEventTypes.FINISHED
                : OperationCaseEventTypes.FAILED;
        insertCaseEvent(job.getCaseId(), eventType, payload, now);
    }

    @Override
    public void bindTaskScan(String caseId, String taskId) {
        if (!StringUtils.hasText(caseId) || !StringUtils.hasText(taskId)) {
            return;
        }
        openTaskRepository.updateCaseId(taskId, caseId);
        Date now = new Date();
        OpenOperationCaseDO patch = new OpenOperationCaseDO();
        patch.setCaseId(caseId);
        patch.setPrimaryResourceType(OpenApiOperations.PRIMARY_RESOURCE_TASK);
        patch.setPrimaryResourceId(taskId);
        patch.setStatus(OperationCaseStatuses.RUNNING);
        patch.setUpdatedAt(now);
        operationCaseRepository.updateProgress(patch);
    }

    @Override
    public void onTaskScanTerminal(OpenTaskDO task) {
        if (task == null || !StringUtils.hasText(task.getCaseId())) {
            return;
        }
        String status = terminalStatusForTask(task.getStatus());
        if (status == null) {
            return;
        }
        Date now = new Date();
        OpenOperationCaseDO patch = new OpenOperationCaseDO();
        patch.setCaseId(task.getCaseId());
        patch.setStatus(status);
        patch.setPrimaryResourceType(OpenApiOperations.PRIMARY_RESOURCE_TASK);
        patch.setPrimaryResourceId(task.getTaskId());
        patch.setErrorMessage(task.getErrorMessage());
        patch.setFinishedAt(now);
        patch.setUpdatedAt(now);
        operationCaseRepository.updateProgress(patch);

        JSONObject payload = new JSONObject();
        payload.put("taskId", task.getTaskId());
        payload.put("taskStatus", task.getStatus());
        payload.put("caseStatus", status);
        String eventType = OperationCaseStatuses.FINISHED.equals(status)
                ? OperationCaseEventTypes.FINISHED
                : OperationCaseEventTypes.FAILED;
        insertCaseEvent(task.getCaseId(), eventType, payload, now);
    }

    private void insertCaseEvent(String caseId, String eventType, JSONObject payload, Date at) {
        OpenOperationCaseEventDO event = new OpenOperationCaseEventDO();
        event.setCaseId(caseId);
        event.setEventType(eventType);
        event.setEventPayloadJson(payload != null ? payload.toJSONString() : null);
        event.setCreatedAt(at);
        operationCaseRepository.insertEvent(event);
    }

    private static String terminalStatusForJob(String jobStatus) {
        if (IVerifyFixJobDomainService.STATUS_FINISHED.equals(jobStatus)) {
            return OperationCaseStatuses.FINISHED;
        }
        if (IVerifyFixJobDomainService.STATUS_FAILED.equals(jobStatus)
                || IVerifyFixJobDomainService.STATUS_DISPATCH_FAILED.equals(jobStatus)) {
            return OperationCaseStatuses.FAILED;
        }
        return null;
    }

    private static String terminalStatusForTask(String taskStatus) {
        if ("FINISHED".equalsIgnoreCase(taskStatus)) {
            return OperationCaseStatuses.FINISHED;
        }
        if ("FAILED".equalsIgnoreCase(taskStatus) || "DISPATCH_FAILED".equalsIgnoreCase(taskStatus)) {
            return OperationCaseStatuses.FAILED;
        }
        return null;
    }

    private String resolveTerminalStatus(boolean success, PrimaryResource primary, ApiResponse<?> response,
                                         String caseType) {
        if (!success) {
            return OperationCaseStatuses.FAILED;
        }
        if (OperationCaseTypes.INSTANCE_BATCH.equals(caseType) && response != null && response.getData() != null) {
            String batchStatus = resolveBatchStatus(response.getData());
            if (batchStatus != null) {
                return batchStatus;
            }
        }
        if (primary.asyncRunning) {
            return OperationCaseStatuses.RUNNING;
        }
        if (primary.taskRunning) {
            return OperationCaseStatuses.RUNNING;
        }
        return OperationCaseStatuses.FINISHED;
    }

    private static String resolveBatchStatus(Object data) {
        try {
            JSONObject json = (JSONObject) JSON.toJSON(data);
            JSONArray successItems = json.getJSONArray("success");
            JSONArray failedItems = json.getJSONArray("failed");
            int successCount = successItems != null ? successItems.size() : 0;
            int failedCount = failedItems != null ? failedItems.size() : 0;
            if (failedCount > 0 && successCount > 0) {
                return OperationCaseStatuses.PARTIAL_FAILED;
            }
            if (failedCount > 0) {
                return OperationCaseStatuses.FAILED;
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private void saveBatchTargets(String caseId, String resultSummaryJson, Date now) {
        if (!StringUtils.hasText(caseId) || !StringUtils.hasText(resultSummaryJson)) {
            return;
        }
        List<OpenOperationCaseTargetDO> existing = operationCaseRepository.listTargetsByCaseId(caseId);
        if (existing != null && !existing.isEmpty()) {
            return;
        }
        try {
            JSONObject data = JSON.parseObject(resultSummaryJson);
            List<OpenOperationCaseTargetDO> targets = new ArrayList<>();
            appendBatchTargets(targets, caseId, data.getJSONArray("success"),
                    OpenOperationCaseTargetDO.STATUS_DONE, now);
            appendBatchTargets(targets, caseId, data.getJSONArray("failed"),
                    OpenOperationCaseTargetDO.STATUS_FAILED, now);
            operationCaseRepository.insertTargets(targets);
        } catch (Exception ignored) {
            // ignore malformed batch response
        }
    }

    private static void appendBatchTargets(List<OpenOperationCaseTargetDO> targets, String caseId,
                                           JSONArray items, String status, Date now) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            JSONObject item = items.getJSONObject(i);
            if (item == null) {
                continue;
            }
            String vulInfoId = firstNonBlank(item.getString("vulInfoID"), item.getString("vulInfoId"));
            if (!StringUtils.hasText(vulInfoId)) {
                continue;
            }
            OpenOperationCaseTargetDO target = new OpenOperationCaseTargetDO();
            target.setCaseId(caseId);
            target.setTargetKey(vulInfoId);
            target.setTargetStatus(status);
            target.setPrevStat(item.getInteger("prevStat"));
            target.setResultStat(item.getInteger("vulInfoStat"));
            if (target.getResultStat() == null) {
                target.setResultStat(item.getInteger("resultStat"));
            }
            target.setPayloadJson(item.toJSONString());
            target.setCreatedAt(now);
            targets.add(target);
        }
    }

    private String resolveTerminalStatus(boolean success, PrimaryResource primary, ApiResponse<?> response) {
        return resolveTerminalStatus(success, primary, response, null);
    }

    private PrimaryResource resolvePrimaryResource(InvocationContext ctx, ApiResponse<?> response) {
        PrimaryResource primary = new PrimaryResource();
        if (StringUtils.hasText(ctx.getResourceType()) && StringUtils.hasText(ctx.getResourceId())
                && !OpenApiOperations.RESOURCE_TYPE_CASE.equals(ctx.getResourceType())) {
            primary.type = mapResourceType(ctx.getResourceType());
            primary.id = ctx.getResourceId();
        }
        if (response == null || response.getData() == null) {
            return primary;
        }
        JSONObject data;
        try {
            data = (JSONObject) JSON.toJSON(response.getData());
        } catch (Exception ex) {
            return primary;
        }
        if (!StringUtils.hasText(primary.id)) {
            primary.id = firstNonBlank(
                    data.getString("taskId"),
                    data.getString("taskID"),
                    data.getString("vulInfoID"),
                    data.getString("vulInfoId"));
            if (StringUtils.hasText(data.getString("taskId")) || StringUtils.hasText(data.getString("taskID"))) {
                primary.type = OpenApiOperations.PRIMARY_RESOURCE_TASK;
            } else if (StringUtils.hasText(data.getString("vulInfoID")) || StringUtils.hasText(data.getString("vulInfoId"))) {
                primary.type = OpenApiOperations.PRIMARY_RESOURCE_INSTANCE;
            }
        }
        String verifyFixJobId = data.getString("verifyFixJobId");
        if (StringUtils.hasText(verifyFixJobId)) {
            primary.type = OpenApiOperations.PRIMARY_RESOURCE_VERIFY_FIX_JOB;
            primary.id = verifyFixJobId;
            String verifyFixStatus = data.getString("verifyFixStatus");
            primary.asyncRunning = "PENDING".equalsIgnoreCase(verifyFixStatus)
                    || "RUNNING".equalsIgnoreCase(verifyFixStatus);
        }
        String taskStatus = data.getString("status");
        if (StringUtils.hasText(taskStatus)) {
            primary.taskRunning = "ACCEPTED".equalsIgnoreCase(taskStatus)
                    || "PENDING".equalsIgnoreCase(taskStatus)
                    || "RUNNING".equalsIgnoreCase(taskStatus);
        }
        primary.batchId = firstNonBlank(data.getString("batchId"), data.getString("batchID"));
        enrichFromBatchResponse(data, primary);
        return primary;
    }

    private void enrichFromBatchResponse(JSONObject data, PrimaryResource primary) {
        JSONArray success = data.getJSONArray("success");
        if (success == null || success.isEmpty()) {
            return;
        }
        JSONObject first = success.getJSONObject(0);
        if (first == null) {
            return;
        }
        String verifyFixJobId = first.getString("verifyFixJobId");
        if (StringUtils.hasText(verifyFixJobId)) {
            primary.type = OpenApiOperations.PRIMARY_RESOURCE_VERIFY_FIX_JOB;
            primary.id = verifyFixJobId;
            String verifyFixStatus = first.getString("verifyFixStatus");
            primary.asyncRunning = "PENDING".equalsIgnoreCase(verifyFixStatus)
                    || "RUNNING".equalsIgnoreCase(verifyFixStatus);
            return;
        }
        if (!StringUtils.hasText(primary.id)) {
            primary.id = firstNonBlank(first.getString("vulInfoID"), first.getString("vulInfoId"));
            if (StringUtils.hasText(primary.id)) {
                primary.type = OpenApiOperations.PRIMARY_RESOURCE_INSTANCE;
            }
        }
    }

    private static String mapResourceType(String resourceType) {
        if (OpenApiOperations.RESOURCE_TYPE_TASK.equals(resourceType)) {
            return OpenApiOperations.PRIMARY_RESOURCE_TASK;
        }
        if (OpenApiOperations.RESOURCE_TYPE_INSTANCE.equals(resourceType)) {
            return OpenApiOperations.PRIMARY_RESOURCE_INSTANCE;
        }
        return resourceType;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static final class PrimaryResource {
        private String type;
        private String id;
        private String batchId;
        private boolean asyncRunning;
        private boolean taskRunning;
    }
}
