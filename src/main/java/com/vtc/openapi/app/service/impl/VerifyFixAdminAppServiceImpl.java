package com.vtc.openapi.app.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.app.service.IVerifyFixAdminAppService;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobItemDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.instance.repository.IOpenVerifyFixJobRepository;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.instance.service.business.IVerifyFixJobDomainService;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskSubRepository;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.MockVerifyFixJobDto;
import com.vtc.openapi.ui.dto.admin.VerifyFixPendingInstanceDto;
import com.vtc.openapi.infra.adapter.taskcenter.TaskCenterVerifyFixProgressService;
import com.vtc.openapi.infra.adapter.taskcenter.TaskCenterVerifyFixOrchestrator;
import com.vtc.openapi.ui.dto.admin.OpenTaskSurveyRefetchResultDto;
import com.vtc.openapi.app.support.TaskScopedInstanceLoader;
import com.vtc.openapi.ui.dto.admin.OpenTaskInstanceScopeDto;
import com.vtc.openapi.ui.dto.admin.VerifyFixWorkspaceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VerifyFixAdminAppServiceImpl implements IVerifyFixAdminAppService {

    private final IOpenVerifyFixJobRepository verifyFixJobRepository;
    private final IOpenVulnInstanceRepository vulnInstanceRepository;
    private final IOpenTaskSubRepository openTaskSubRepository;
    private final IVerifyFixJobDomainService verifyFixJobDomainService;
    private final VerifyFixWorkspaceAssembler workspaceAssembler;
    private final TaskScopedInstanceLoader taskScopedInstanceLoader;
    private final TaskCenterVerifyFixProgressService verifyFixProgressService;
    private final TaskCenterVerifyFixOrchestrator verifyFixOrchestrator;

    public VerifyFixAdminAppServiceImpl(IOpenVerifyFixJobRepository verifyFixJobRepository,
                                        IOpenVulnInstanceRepository vulnInstanceRepository,
                                        IOpenTaskSubRepository openTaskSubRepository,
                                        IVerifyFixJobDomainService verifyFixJobDomainService,
                                        VerifyFixWorkspaceAssembler workspaceAssembler,
                                        TaskScopedInstanceLoader taskScopedInstanceLoader,
                                        @Autowired(required = false) TaskCenterVerifyFixProgressService verifyFixProgressService,
                                        @Autowired(required = false) TaskCenterVerifyFixOrchestrator verifyFixOrchestrator) {
        this.verifyFixJobRepository = verifyFixJobRepository;
        this.vulnInstanceRepository = vulnInstanceRepository;
        this.openTaskSubRepository = openTaskSubRepository;
        this.verifyFixJobDomainService = verifyFixJobDomainService;
        this.workspaceAssembler = workspaceAssembler;
        this.taskScopedInstanceLoader = taskScopedInstanceLoader;
        this.verifyFixProgressService = verifyFixProgressService;
        this.verifyFixOrchestrator = verifyFixOrchestrator;
    }

    @Override
    public ApiResponse<List<MockVerifyFixJobDto>> listJobs(String partnerId, String status, String taskId,
                                                            String jobId, int limit) {
        int cap = Math.max(1, Math.min(limit, 200));
        List<OpenVerifyFixJobDO> rows = verifyFixJobRepository.listForAdmin(partnerId, status, jobId, cap);
        List<MockVerifyFixJobDto> dtos = new ArrayList<>();
        String taskFilter = StringUtils.hasText(taskId) ? taskId.trim() : null;
        for (OpenVerifyFixJobDO row : rows) {
            if (row == null) {
                continue;
            }
            if (taskFilter != null && !jobContainsTask(row.getJobId(), taskFilter)) {
                continue;
            }
            dtos.add(workspaceAssembler.toListDto(row));
        }
        return ApiResponse.ok(dtos);
    }

    @Override
    public ApiResponse<VerifyFixWorkspaceDto> getWorkspace(String jobId) {
        if (!StringUtils.hasText(jobId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "jobId 不能为空");
        }
        VerifyFixWorkspaceDto workspace = workspaceAssembler.build(jobId.trim());
        if (workspace == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "修复核验任务不存在");
        }
        return ApiResponse.ok(workspace);
    }

    @Override
    public ApiResponse<OpenTaskInstanceScopeDto> getJobInstances(String jobId, String taskId, String subId) {
        if (!StringUtils.hasText(jobId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "jobId 不能为空");
        }
        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(subId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "taskId/subId 不能为空");
        }
        OpenVerifyFixJobDO job = verifyFixJobRepository.findByJobId(jobId.trim());
        if (job == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "修复核验任务不存在");
        }
        List<OpenVerifyFixJobItemDO> items = verifyFixJobRepository.listItemsByJobId(jobId.trim());
        return ApiResponse.ok(taskScopedInstanceLoader.loadVerifyFixScope(
                job, taskId.trim(), subId.trim(), items, 500));
    }

    @Override
    public ApiResponse<OpenTaskSurveyRefetchResultDto> refetchRescanSub(String jobId, String subId) {
        if (verifyFixProgressService == null) {
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID, "仅 task-center 模式支持重新获取复扫结果");
        }
        return ApiResponse.ok(verifyFixProgressService.refetchRescanSub(jobId, subId));
    }

    @Override
    public ApiResponse<Boolean> retryDispatch(String jobId) {
        if (!StringUtils.hasText(jobId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "jobId 不能为空");
        }
        if (verifyFixOrchestrator == null) {
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID, "仅 task-center 模式支持手动重试下发");
        }
        OpenVerifyFixJobDO job = verifyFixJobRepository.findByJobId(jobId.trim());
        if (job == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "修复核验任务不存在");
        }
        // 手动重试不受自动重试 5 次上限约束；复用已有 sub 重新下发，不堆积新 sub
        boolean ok = verifyFixOrchestrator.retryDispatchForJob(jobId.trim());
        if (!ok) {
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID,
                    "当前任务不可手动重试（已终态或无待重试子任务）");
        }
        // 手动重试成功后重置自动重试计数，避免残留计数影响后续自动调度
        OpenVerifyFixJobDO latest = verifyFixJobRepository.findByJobId(jobId.trim());
        if (latest != null && latest.getRetryCount() != null && latest.getRetryCount() > 0) {
            latest.setRetryCount(0);
            latest.setUpdatedAt(new java.util.Date());
            verifyFixJobRepository.updateJob(latest);
        }
        return ApiResponse.ok(true);
    }

    @Override
    public ApiResponse<Boolean> retryDispatchSub(String jobId, String subId) {
        if (!StringUtils.hasText(jobId) || !StringUtils.hasText(subId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "jobId/subId 不能为空");
        }
        if (verifyFixOrchestrator == null) {
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID, "仅 task-center 模式支持手动重试下发");
        }
        boolean ok = verifyFixOrchestrator.retryDispatchForSub(jobId.trim(), subId.trim());
        if (!ok) {
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID,
                    "该子任务不可重试（不存在、非本任务、已成功下发或任务已终态）");
        }
        return ApiResponse.ok(true);
    }

    private boolean jobContainsTask(String jobId, String taskId) {
        List<OpenVerifyFixJobItemDO> items = verifyFixJobRepository.listItemsByJobId(jobId);
        if (CollUtil.isEmpty(items)) {
            return false;
        }
        for (OpenVerifyFixJobItemDO item : items) {
            if (item != null && taskId.equals(item.getTaskId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ApiResponse<List<VerifyFixPendingInstanceDto>> listPendingInstances(String partnerId, String taskId,
                                                                              String jobId, int limit) {
        if (!StringUtils.hasText(partnerId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "partnerId 不能为空");
        }
        int cap = Math.max(1, Math.min(limit, 500));
        List<OpenVerifyFixJobDO> jobs;
        if (StringUtils.hasText(jobId)) {
            OpenVerifyFixJobDO one = verifyFixJobRepository.findByJobId(jobId.trim());
            jobs = one != null ? Collections.singletonList(one) : Collections.emptyList();
        } else {
            jobs = verifyFixJobRepository.listByPartner(partnerId.trim(), null, cap);
        }
        List<VerifyFixPendingInstanceDto> rows = new ArrayList<>();
        Map<String, OpenTaskSubDO> subCache = new HashMap<>();
        for (OpenVerifyFixJobDO job : jobs) {
            if (job == null || !partnerId.trim().equals(job.getPartnerId())) {
                continue;
            }
            if (IVerifyFixJobDomainService.STATUS_FINISHED.equals(job.getStatus())
                    || IVerifyFixJobDomainService.STATUS_FAILED.equals(job.getStatus())) {
                continue;
            }
            List<OpenVerifyFixJobItemDO> items = verifyFixJobRepository.listItemsByJobId(job.getJobId());
            for (OpenVerifyFixJobItemDO item : items) {
                if (item == null || !IVerifyFixJobDomainService.ITEM_PENDING.equals(item.getItemStatus())) {
                    continue;
                }
                if (StringUtils.hasText(taskId) && !taskId.trim().equals(item.getTaskId())) {
                    continue;
                }
                OpenVulnInstanceDO inst = vulnInstanceRepository.findByPartnerAndVulInfoId(
                        partnerId.trim(), item.getVulInfoId());
                if (inst == null || inst.getVulInfoStat() == null || inst.getVulInfoStat() != 5) {
                    continue;
                }
                rows.add(toPendingDto(job, item, inst, subCache));
                if (rows.size() >= cap) {
                    return ApiResponse.ok(rows);
                }
            }
        }
        return ApiResponse.ok(rows);
    }

    private VerifyFixPendingInstanceDto toPendingDto(OpenVerifyFixJobDO job,
                                                     OpenVerifyFixJobItemDO item,
                                                     OpenVulnInstanceDO inst,
                                                     Map<String, OpenTaskSubDO> subCache) {
        VerifyFixPendingInstanceDto dto = new VerifyFixPendingInstanceDto();
        dto.setVulInfoId(item.getVulInfoId());
        dto.setTaskId(item.getTaskId());
        dto.setVerifyFixJobId(job.getJobId());
        dto.setCaseId(job.getCaseId());
        dto.setSourceSubId(item.getSourceSubId());
        dto.setScannerType(item.getScannerType());
        dto.setRescanSubId(item.getRescanSubId());
        dto.setItemStatus(item.getItemStatus());
        dto.setVulInfoStat(inst.getVulInfoStat());
        fillSnapshot(dto, inst.getSnapshotJson());
        if (StringUtils.hasText(item.getRescanSubId())) {
            OpenTaskSubDO sub = subCache.computeIfAbsent(item.getRescanSubId(),
                    openTaskSubRepository::findBySubId);
            if (sub != null) {
                dto.setRescanSubStatus(sub.getStatus());
                dto.setRescanProgress(sub.getProgress());
                dto.setReportDownloadPath(sub.getReportDownloadPath());
                if (!StringUtils.hasText(dto.getScannerType())) {
                    dto.setScannerType(sub.getScannerType());
                }
            }
        }
        return dto;
    }

    private static void fillSnapshot(VerifyFixPendingInstanceDto dto, String snapshotJson) {
        if (!StringUtils.hasText(snapshotJson)) {
            return;
        }
        JSONObject snap = JSON.parseObject(snapshotJson);
        if (snap == null) {
            return;
        }
        dto.setVulName(firstOf(snap.getString("vulName"), snap.getString("name")));
        dto.setVulNetAddr(firstOf(snap.getString("vulNetAddr"), snap.getString("vulNetAddress")));
        dto.setVulPort(snap.getInteger("vulPort"));
        dto.setVulId(firstOf(snap.getString("vulID"), snap.getString("vulId")));
    }

    private static String firstOf(String a, String b) {
        return StringUtils.hasText(a) ? a : b;
    }
}
