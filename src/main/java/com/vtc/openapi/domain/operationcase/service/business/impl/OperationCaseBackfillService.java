package com.vtc.openapi.domain.operationcase.service.business.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiOperations;
import com.vtc.openapi.domain.open.model.entity.ApiInvocationDO;
import com.vtc.openapi.domain.open.repository.IApiInvocationRepository;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobDO;
import com.vtc.openapi.domain.instance.repository.IOpenVerifyFixJobRepository;
import com.vtc.openapi.domain.operationcase.model.OperationCaseEventTypes;
import com.vtc.openapi.domain.operationcase.model.OperationCaseStatuses;
import com.vtc.openapi.domain.operationcase.model.OperationCaseTypes;
import com.vtc.openapi.domain.operationcase.model.entity.OpenOperationCaseDO;
import com.vtc.openapi.domain.operationcase.model.entity.OpenOperationCaseEventDO;
import com.vtc.openapi.domain.operationcase.model.support.OperationCaseSupport;
import com.vtc.openapi.domain.operationcase.repository.IOpenOperationCaseRepository;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.ui.dto.admin.OperationCaseBackfillResultDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * W4：历史数据回填 — 从 api_invocation / open_task / open_verify_fix_job 补建或互链 case_id。
 */
@Service
public class OperationCaseBackfillService {

    private final IOpenOperationCaseRepository operationCaseRepository;
    private final IApiInvocationRepository apiInvocationRepository;
    private final IOpenTaskRepository openTaskRepository;
    private final IOpenVerifyFixJobRepository verifyFixJobRepository;

    public OperationCaseBackfillService(IOpenOperationCaseRepository operationCaseRepository,
                                        IApiInvocationRepository apiInvocationRepository,
                                        IOpenTaskRepository openTaskRepository,
                                        IOpenVerifyFixJobRepository verifyFixJobRepository) {
        this.operationCaseRepository = operationCaseRepository;
        this.apiInvocationRepository = apiInvocationRepository;
        this.openTaskRepository = openTaskRepository;
        this.verifyFixJobRepository = verifyFixJobRepository;
    }

    public OperationCaseBackfillResultDto backfill(String partnerId, int limit, boolean dryRun) {
        int capped = limit > 0 ? Math.min(limit, 500) : 200;
        OperationCaseBackfillResultDto result = new OperationCaseBackfillResultDto();
        result.setDryRun(dryRun);
        result.setLimit(capped);

        List<ApiInvocationDO> orphans = apiInvocationRepository.listCaseOperationsWithoutCaseId(
                partnerId, OperationCaseSupport.caseOperationIds(), capped);
        for (ApiInvocationDO inv : orphans) {
            if (operationCaseRepository.findByInvocationId(inv.getInvocationId()) != null) {
                continue;
            }
            if (!dryRun) {
                createCaseFromInvocation(inv);
            }
            result.setCasesCreated(result.getCasesCreated() + 1);
        }

        List<OpenOperationCaseDO> cases = operationCaseRepository.listRecent(partnerId, capped);
        for (OpenOperationCaseDO row : cases) {
            if (!StringUtils.hasText(row.getInvocationId())) {
                continue;
            }
            ApiInvocationDO inv = apiInvocationRepository.findByInvocationId(row.getInvocationId());
            if (inv == null || StringUtils.hasText(inv.getCaseId())) {
                continue;
            }
            if (!dryRun) {
                apiInvocationRepository.updateCaseId(inv.getInvocationId(), row.getCaseId());
            }
            result.setInvocationsLinked(result.getInvocationsLinked() + 1);
        }

        for (OpenOperationCaseDO row : cases) {
            if (!StringUtils.hasText(row.getPrimaryResourceId())) {
                continue;
            }
            if (OperationCaseTypes.TASK_SCAN.equals(row.getCaseType())) {
                OpenTaskDO task = openTaskRepository.findByTaskId(row.getPrimaryResourceId());
                if (task != null && !StringUtils.hasText(task.getCaseId())) {
                    if (!dryRun) {
                        openTaskRepository.updateCaseId(task.getTaskId(), row.getCaseId());
                    }
                    result.setTasksLinked(result.getTasksLinked() + 1);
                }
            } else if (OperationCaseTypes.VERIFY_FIX.equals(row.getCaseType())) {
                OpenVerifyFixJobDO job = verifyFixJobRepository.findByJobId(row.getPrimaryResourceId());
                if (job != null && !StringUtils.hasText(job.getCaseId())) {
                    if (!dryRun) {
                        verifyFixJobRepository.updateCaseId(job.getJobId(), row.getCaseId());
                    }
                    result.setJobsLinked(result.getJobsLinked() + 1);
                }
            }
        }

        List<OpenTaskDO> tasks = openTaskRepository.listWithoutCaseId(capped);
        for (OpenTaskDO task : tasks) {
            OpenOperationCaseDO row = operationCaseRepository.findByPrimaryResource(
                    task.getPartnerId(), OpenApiOperations.PRIMARY_RESOURCE_TASK, task.getTaskId());
            if (row != null) {
                if (!dryRun) {
                    openTaskRepository.updateCaseId(task.getTaskId(), row.getCaseId());
                }
                result.setTasksLinked(result.getTasksLinked() + 1);
            }
        }

        List<OpenVerifyFixJobDO> jobs = verifyFixJobRepository.listWithoutCaseId(capped);
        for (OpenVerifyFixJobDO job : jobs) {
            OpenOperationCaseDO row = operationCaseRepository.findByPrimaryResource(
                    job.getPartnerId(), OpenApiOperations.PRIMARY_RESOURCE_VERIFY_FIX_JOB, job.getJobId());
            if (row != null) {
                if (!dryRun) {
                    verifyFixJobRepository.updateCaseId(job.getJobId(), row.getCaseId());
                }
                result.setJobsLinked(result.getJobsLinked() + 1);
            }
        }

        return result;
    }

    private void createCaseFromInvocation(ApiInvocationDO inv) {
        String caseType = OperationCaseSupport.resolveCaseType(inv.getOperationId());
        if (!StringUtils.hasText(caseType)) {
            return;
        }
        String caseId = "CASE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Date now = inv.getFinishedAt() != null ? inv.getFinishedAt() : inv.getStartedAt();
        if (now == null) {
            now = new Date();
        }

        OpenOperationCaseDO row = new OpenOperationCaseDO();
        row.setCaseId(caseId);
        row.setPartnerId(inv.getPartnerId());
        row.setCaseType(caseType);
        row.setStatus(resolveStatusFromInvocation(inv));
        row.setTitle(OperationCaseSupport.buildTitle(caseType, inv.getOperationId()) + "（回填）");
        row.setInvocationId(inv.getInvocationId());
        row.setStartedAt(inv.getStartedAt() != null ? inv.getStartedAt() : now);
        row.setFinishedAt(inv.getFinishedAt());
        row.setErrorMessage(inv.getErrorMessage());
        row.setCreatedAt(now);
        row.setUpdatedAt(now);

        if (StringUtils.hasText(inv.getResourceId())) {
            row.setPrimaryResourceId(inv.getResourceId());
            row.setPrimaryResourceType(mapResourceType(inv.getResourceType(), caseType));
        }
        if (StringUtils.hasText(inv.getResponseBodyJson())) {
            row.setResultSummaryJson(OperationCaseSupport.truncate(inv.getResponseBodyJson(), 2048));
            enrichPrimaryFromResponse(row, inv.getResponseBodyJson());
        }

        operationCaseRepository.insert(row);
        apiInvocationRepository.updateCaseId(inv.getInvocationId(), caseId);

        OpenOperationCaseEventDO event = new OpenOperationCaseEventDO();
        event.setCaseId(caseId);
        event.setEventType(OperationCaseEventTypes.ACCEPTED);
        event.setEventPayloadJson("{\"source\":\"backfill\",\"invocationId\":\"" + inv.getInvocationId() + "\"}");
        event.setCreatedAt(now);
        operationCaseRepository.insertEvent(event);

        if (OperationCaseTypes.TASK_SCAN.equals(caseType) && StringUtils.hasText(row.getPrimaryResourceId())) {
            openTaskRepository.updateCaseId(row.getPrimaryResourceId(), caseId);
        } else if (OperationCaseTypes.VERIFY_FIX.equals(caseType) && StringUtils.hasText(row.getPrimaryResourceId())) {
            verifyFixJobRepository.updateCaseId(row.getPrimaryResourceId(), caseId);
        }
    }

    private static String resolveStatusFromInvocation(ApiInvocationDO inv) {
        if (inv.getResponseCode() == null || inv.getResponseCode() != OpenApiConstants.CODE_OK) {
            return OperationCaseStatuses.FAILED;
        }
        return OperationCaseStatuses.FINISHED;
    }

    private static String mapResourceType(String resourceType, String caseType) {
        if (OpenApiOperations.RESOURCE_TYPE_TASK.equals(resourceType)) {
            return OpenApiOperations.PRIMARY_RESOURCE_TASK;
        }
        if (OpenApiOperations.RESOURCE_TYPE_INSTANCE.equals(resourceType)) {
            return OpenApiOperations.PRIMARY_RESOURCE_INSTANCE;
        }
        if (OperationCaseTypes.TASK_SCAN.equals(caseType)) {
            return OpenApiOperations.PRIMARY_RESOURCE_TASK;
        }
        if (OperationCaseTypes.VERIFY_FIX.equals(caseType)) {
            return OpenApiOperations.PRIMARY_RESOURCE_VERIFY_FIX_JOB;
        }
        return resourceType;
    }

    private static void enrichPrimaryFromResponse(OpenOperationCaseDO row, String responseBodyJson) {
        try {
            JSONObject data = JSON.parseObject(responseBodyJson);
            if (!StringUtils.hasText(row.getPrimaryResourceId())) {
                String taskId = firstNonBlank(data.getString("taskId"), data.getString("taskID"));
                if (StringUtils.hasText(taskId)) {
                    row.setPrimaryResourceId(taskId);
                    row.setPrimaryResourceType(OpenApiOperations.PRIMARY_RESOURCE_TASK);
                }
                String jobId = data.getString("verifyFixJobId");
                if (StringUtils.hasText(jobId)) {
                    row.setPrimaryResourceId(jobId);
                    row.setPrimaryResourceType(OpenApiOperations.PRIMARY_RESOURCE_VERIFY_FIX_JOB);
                }
            }
            String batchId = firstNonBlank(data.getString("batchId"), data.getString("batchID"));
            if (StringUtils.hasText(batchId)) {
                row.setBatchId(batchId);
            }
        } catch (Exception ignored) {
            // ignore malformed json
        }
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
}
