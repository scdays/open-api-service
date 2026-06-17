package com.vtc.openapi.app.service.impl;

import com.vtc.openapi.app.service.IMockTaskAdminAppService;
import com.vtc.openapi.app.service.IMockVerifyFixAdminAppService;
import com.vtc.openapi.app.support.VerifyFixInvocationCandidateResolver;
import com.vtc.openapi.app.support.VerifyFixInvocationCandidateResolver.ExtractedCandidate;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.open.OpenApiOperations;
import com.vtc.openapi.domain.open.model.entity.ApiInvocationDO;
import com.vtc.openapi.domain.open.repository.IApiInvocationRepository;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobItemDO;
import com.vtc.openapi.domain.instance.model.support.VerifyFixCompleteMode;
import com.vtc.openapi.domain.instance.repository.IOpenVerifyFixJobRepository;
import com.vtc.openapi.domain.instance.service.business.IVerifyFixJobDomainService;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.infra.converter.InstanceItemConverter;
import com.vtc.openapi.domain.instance.model.result.InstanceItemResult;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.MockVerifyFixCompleteResultDto;
import com.vtc.openapi.ui.dto.admin.MockVerifyFixJobDto;
import com.vtc.openapi.ui.dto.admin.MockVerifyFixJobItemDto;
import com.vtc.openapi.ui.dto.admin.MockVulnInstanceOpsRowDto;
import com.vtc.openapi.ui.dto.admin.MockBundleStatusDto;
import com.vtc.openapi.ui.dto.admin.OfflineTaskVerifyFixContextDto;
import com.vtc.openapi.ui.dto.admin.VerifyFixInvocationCandidateDto;
import com.vtc.openapi.ui.params.admin.CreateInternalVerifyFixJobParams;
import com.vtc.openapi.ui.params.admin.CreateVerifyFixJobFromSelectionParams;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "mock")
public class MockVerifyFixAdminAppServiceImpl implements IMockVerifyFixAdminAppService {

    private static final SimpleDateFormat ISO_UTC;

    static {
        ISO_UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        ISO_UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final IVerifyFixJobDomainService verifyFixJobDomainService;
    private final IOpenTaskRepository openTaskRepository;
    private final IOpenVulnInstanceRepository vulnInstanceRepository;
    private final IMockTaskAdminAppService mockTaskAdminAppService;
    private final IApiInvocationRepository apiInvocationRepository;
    private final IOpenVerifyFixJobRepository verifyFixJobRepository;

    public MockVerifyFixAdminAppServiceImpl(IVerifyFixJobDomainService verifyFixJobDomainService,
                                            IOpenTaskRepository openTaskRepository,
                                            IOpenVulnInstanceRepository vulnInstanceRepository,
                                            IMockTaskAdminAppService mockTaskAdminAppService,
                                            IApiInvocationRepository apiInvocationRepository,
                                            IOpenVerifyFixJobRepository verifyFixJobRepository) {
        this.verifyFixJobDomainService = verifyFixJobDomainService;
        this.openTaskRepository = openTaskRepository;
        this.vulnInstanceRepository = vulnInstanceRepository;
        this.mockTaskAdminAppService = mockTaskAdminAppService;
        this.apiInvocationRepository = apiInvocationRepository;
        this.verifyFixJobRepository = verifyFixJobRepository;
    }

    @Override
    public ApiResponse<List<MockVerifyFixJobDto>> listJobs(String partnerId, String status, int limit) {
        List<OpenVerifyFixJobDO> rows = verifyFixJobDomainService.listRecentJobs(partnerId, status, limit);
        List<MockVerifyFixJobDto> dtos = new ArrayList<>();
        for (OpenVerifyFixJobDO row : rows) {
            dtos.add(toDto(row, false));
        }
        return ApiResponse.ok(dtos);
    }

    @Override
    public ApiResponse<MockVerifyFixJobDto> getJob(String jobId) {
        OpenVerifyFixJobDO job = verifyFixJobDomainService.requireJob(jobId);
        return ApiResponse.ok(toDto(job, true));
    }

    @Override
    public ApiResponse<MockVerifyFixCompleteResultDto> importRescanXml(String jobId, MultipartFile file) {
        byte[] bytes = readXml(file);
        verifyFixJobDomainService.importRescanXmlAndComplete(jobId, bytes);
        return ApiResponse.ok(toCompleteResult(jobId, "已导入复扫 XML 并完成比对"));
    }

    @Override
    public ApiResponse<MockVerifyFixCompleteResultDto> completeAllFixed(String jobId) {
        verifyFixJobDomainService.completeJob(jobId, VerifyFixCompleteMode.ALL_FIXED);
        return ApiResponse.ok(toCompleteResult(jobId, "已全部标记为核验修复(6)"));
    }

    @Override
    public ApiResponse<MockVerifyFixCompleteResultDto> completeAllUnfixed(String jobId) {
        verifyFixJobDomainService.completeJob(jobId, VerifyFixCompleteMode.ALL_UNFIXED);
        return ApiResponse.ok(toCompleteResult(jobId, "已全部标记为核验未修复(7)"));
    }

    @Override
    public ApiResponse<MockVerifyFixCompleteResultDto> completeByCompare(String jobId) {
        verifyFixJobDomainService.completeJob(jobId, VerifyFixCompleteMode.COMPARE_RESCAN);
        return ApiResponse.ok(toCompleteResult(jobId, "已按复扫报告比对完成"));
    }

    @Override
    public ApiResponse<OfflineTaskVerifyFixContextDto> getOfflineTaskContext(String partnerId, String taskId) {
        if (!StringUtils.hasText(partnerId) || !StringUtils.hasText(taskId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "partnerId/taskId 不能为空");
        }
        String normalizedTaskId = taskId.trim();
        OpenTaskDO task = openTaskRepository.findByTaskId(normalizedTaskId);
        if (task == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "任务不存在");
        }
        if (!partnerId.equals(task.getPartnerId())) {
            throw new OpenApiException(OpenApiConstants.CODE_CROSS_PARTNER, "任务不属于该接入方");
        }
        OfflineTaskVerifyFixContextDto dto = new OfflineTaskVerifyFixContextDto();
        dto.setTaskId(normalizedTaskId);
        dto.setExtTaskId(task.getExtTaskId());
        dto.setPartnerId(partnerId);
        dto.setTaskStatus(task.getStatus());
        MockBundleStatusDto bundle = mockTaskAdminAppService.getBundleStatus(normalizedTaskId).getData();
        if (bundle != null) {
            dto.setInstancesIngested(bundle.getInstancesIngested());
            dto.setPersistedInstanceCount((int) bundle.getPersistedInstanceCount());
            dto.setHasSourceXml(bundle.isHasSourceXml());
        }
        Map<String, Integer> statCounts = new LinkedHashMap<>();
        List<String> eligible = new ArrayList<>();
        List<OpenVulnInstanceDO> rows = vulnInstanceRepository.listByPartnerAndTask(
                partnerId, normalizedTaskId, null);
        for (OpenVulnInstanceDO row : rows) {
            String key = row.getVulInfoStat() == null ? "null" : String.valueOf(row.getVulInfoStat());
            statCounts.put(key, statCounts.getOrDefault(key, 0) + 1);
            if (row.getVulInfoStat() != null && row.getVulInfoStat() == 5) {
                eligible.add(row.getVulInfoId());
            }
        }
        dto.setStatCounts(statCounts);
        dto.setEligibleVulInfoIds(eligible);
        return ApiResponse.ok(dto);
    }

    @Override
    public ApiResponse<MockVerifyFixJobDto> createFromOfflineTask(CreateInternalVerifyFixJobParams params) {
        String jobId = verifyFixJobDomainService.createInternalFromOfflineTask(
                params.getPartnerId(),
                params.getTaskId(),
                params.getVulInfoIds(),
                params.getBatchId());
        return ApiResponse.ok(toDto(verifyFixJobDomainService.requireJob(jobId), true));
    }

    @Override
    public ApiResponse<List<MockVulnInstanceOpsRowDto>> listInstancesForOps(String partnerId, String taskId,
                                                                           Integer vulInfoStat, int limit) {
        if (!StringUtils.hasText(partnerId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "partnerId 不能为空");
        }
        int capped = Math.max(1, Math.min(limit, 500));
        String normalizedTaskId = StringUtils.hasText(taskId) ? taskId.trim() : null;
        List<OpenVulnInstanceDO> rows = vulnInstanceRepository.listByPartner(
                partnerId.trim(), normalizedTaskId, vulInfoStat, capped);
        List<MockVulnInstanceOpsRowDto> result = new ArrayList<>();
        for (OpenVulnInstanceDO row : rows) {
            MockVulnInstanceOpsRowDto dto = new MockVulnInstanceOpsRowDto();
            dto.setVulInfoId(row.getVulInfoId());
            dto.setVulInfoStat(row.getVulInfoStat());
            dto.setTaskId(row.getTaskId());
            InstanceItemResult item = InstanceItemConverter.fromSnapshot(row);
            if (item != null) {
                dto.setVulName(item.getVulName());
                dto.setVulNetAddr(item.getVulNetAddr());
                dto.setVulPort(item.getVulPort());
            }
            OpenVerifyFixJobItemDO pending = verifyFixJobRepository
                    .findLatestPendingItemByPartnerAndVulInfoId(partnerId.trim(), row.getVulInfoId());
            if (pending != null && StringUtils.hasText(pending.getJobId())) {
                dto.setPendingVerifyFixJobId(pending.getJobId());
            }
            result.add(dto);
        }
        return ApiResponse.ok(result);
    }

    @Override
    public ApiResponse<List<VerifyFixInvocationCandidateDto>> listInvocationCandidates(String partnerId, int limit) {
        if (!StringUtils.hasText(partnerId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "partnerId 不能为空");
        }
        int capped = Math.max(1, Math.min(limit, 200));
        List<ApiInvocationDO> invocations = apiInvocationRepository.listRecentByPartnerAndOperations(
                partnerId.trim(),
                Arrays.asList(
                        OpenApiOperations.VERIFY_FIX_INSTANCE,
                        OpenApiOperations.VERIFY_FIX_INSTANCE_BATCH),
                OpenApiConstants.CODE_OK,
                capped * 3);
        Map<String, VerifyFixInvocationCandidateDto> merged = new LinkedHashMap<>();
        for (ApiInvocationDO invocation : invocations) {
            String responseBody = invocation.getResponseBodyJson();
            if (!StringUtils.hasText(responseBody)) {
                responseBody = apiInvocationRepository.findResponseBodyJson(invocation.getInvocationId());
            }
            String requestBody = invocation.getRequestBodyJson();
            if (!StringUtils.hasText(requestBody)) {
                requestBody = apiInvocationRepository.findRequestBodyJson(invocation.getInvocationId());
            }
            ApiInvocationDO enriched = invocation;
            if (responseBody != null && !responseBody.equals(invocation.getResponseBodyJson())) {
                enriched = copyInvocation(invocation, requestBody, responseBody);
            } else if (requestBody != null && !requestBody.equals(invocation.getRequestBodyJson())) {
                enriched = copyInvocation(invocation, requestBody, responseBody);
            }
            for (ExtractedCandidate extracted : VerifyFixInvocationCandidateResolver.extract(enriched)) {
                if (!StringUtils.hasText(extracted.vulInfoId)) {
                    continue;
                }
                VerifyFixInvocationCandidateDto dto = merged.computeIfAbsent(
                        extracted.vulInfoId, key -> new VerifyFixInvocationCandidateDto());
                dto.setVulInfoId(extracted.vulInfoId);
                dto.setInvocationId(extracted.invocationId);
                dto.setOperationId(extracted.operationId);
                dto.setInvokedAt(formatUtc(extracted.invokedAt));
                if (StringUtils.hasText(extracted.verifyFixJobId)) {
                    dto.setVerifyFixJobId(extracted.verifyFixJobId);
                }
                OpenVulnInstanceDO instance = vulnInstanceRepository.findByPartnerAndVulInfoId(
                        partnerId.trim(), extracted.vulInfoId);
                if (instance != null) {
                    dto.setTaskId(instance.getTaskId());
                    dto.setVulInfoStat(instance.getVulInfoStat());
                }
            }
        }
        for (VerifyFixInvocationCandidateDto dto : merged.values()) {
            enrichJobStatus(partnerId.trim(), dto);
        }
        List<VerifyFixInvocationCandidateDto> rows = new ArrayList<>(merged.values());
        if (rows.size() > capped) {
            rows = rows.subList(0, capped);
        }
        return ApiResponse.ok(rows);
    }

    @Override
    public ApiResponse<MockVerifyFixJobDto> createFromSelection(CreateVerifyFixJobFromSelectionParams params) {
        String jobId = verifyFixJobDomainService.createJobFromSelection(
                params.getPartnerId(),
                params.getVulInfoIds(),
                params.getBatchId());
        return ApiResponse.ok(toDto(verifyFixJobDomainService.requireJob(jobId), true));
    }

    private void enrichJobStatus(String partnerId, VerifyFixInvocationCandidateDto dto) {
        if (!StringUtils.hasText(dto.getVerifyFixJobId())) {
            OpenVerifyFixJobItemDO pending = verifyFixJobRepository
                    .findLatestPendingItemByPartnerAndVulInfoId(partnerId, dto.getVulInfoId());
            if (pending != null && StringUtils.hasText(pending.getJobId())) {
                dto.setVerifyFixJobId(pending.getJobId());
            }
        }
        if (!StringUtils.hasText(dto.getVerifyFixJobId())) {
            return;
        }
        OpenVerifyFixJobDO job = verifyFixJobRepository.findByJobId(dto.getVerifyFixJobId());
        if (job != null) {
            dto.setJobStatus(job.getStatus());
        }
    }

    private MockVerifyFixJobDto toDto(OpenVerifyFixJobDO job, boolean withItems) {
        MockVerifyFixJobDto dto = new MockVerifyFixJobDto();
        dto.setJobId(job.getJobId());
        dto.setPartnerId(job.getPartnerId());
        dto.setBatchId(job.getBatchId());
        dto.setStatus(job.getStatus());
        dto.setItemCount(job.getItemCount());
        dto.setRescanImported(job.getRescanImported());
        dto.setErrorMessage(job.getErrorMessage());
        dto.setFinishedAt(formatUtc(job.getFinishedAt()));
        dto.setCreatedAt(formatUtc(job.getCreatedAt()));
        if (withItems) {
            List<OpenVerifyFixJobItemDO> items = verifyFixJobDomainService.listJobItems(job.getJobId());
            for (OpenVerifyFixJobItemDO item : items) {
                MockVerifyFixJobItemDto row = new MockVerifyFixJobItemDto();
                row.setVulInfoId(item.getVulInfoId());
                row.setTaskId(item.getTaskId());
                row.setPreviousStat(item.getPreviousStat());
                row.setResultStat(item.getResultStat());
                row.setItemStatus(item.getItemStatus());
                dto.getItems().add(row);
            }
        }
        return dto;
    }

    private MockVerifyFixCompleteResultDto toCompleteResult(String jobId, String message) {
        OpenVerifyFixJobDO job = verifyFixJobDomainService.requireJob(jobId);
        MockVerifyFixCompleteResultDto dto = new MockVerifyFixCompleteResultDto();
        dto.setJobId(jobId);
        dto.setStatus(job.getStatus());
        dto.setMessage(message);
        return dto;
    }

    private static byte[] readXml(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "XML file is required");
        }
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "read XML failed: " + ex.getMessage());
        }
    }

    private static ApiInvocationDO copyInvocation(ApiInvocationDO source, String requestBody, String responseBody) {
        ApiInvocationDO copy = new ApiInvocationDO();
        copy.setInvocationId(source.getInvocationId());
        copy.setRequestId(source.getRequestId());
        copy.setPartnerId(source.getPartnerId());
        copy.setOperationId(source.getOperationId());
        copy.setResourceId(source.getResourceId());
        copy.setResourceType(source.getResourceType());
        copy.setStartedAt(source.getStartedAt());
        copy.setFinishedAt(source.getFinishedAt());
        copy.setRequestBodyJson(requestBody);
        copy.setResponseBodyJson(responseBody);
        return copy;
    }

    private static String formatUtc(Date date) {
        if (date == null) {
            return null;
        }
        synchronized (ISO_UTC) {
            return ISO_UTC.format(date);
        }
    }
}
