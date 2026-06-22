package com.vtc.openapi.app.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.botany.spore.core.page.PageInfo;
import com.vtc.openapi.app.convert.AdminGovernanceAppConvertor;
import com.vtc.openapi.app.service.IOpenTaskAdminAppService;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.instance.model.result.InstanceItemResult;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.open.OpenApiOperations;
import com.vtc.openapi.domain.open.model.entity.WebhookDeliveryLogDO;
import com.vtc.openapi.domain.open.repository.IApiInvocationRepository;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.task.model.query.OpenTaskAdminQuery;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.domain.task.repository.IOpenTaskSubRepository;
import com.vtc.openapi.domain.export.service.business.IExportDownloadPolicy;
import com.vtc.openapi.infra.adapter.taskcenter.TaskCenterReportArchiveService;
import com.vtc.openapi.infra.adapter.taskcenter.TaskCenterScanResultQueryService;
import com.vtc.openapi.infra.adapter.taskcenter.TaskCenterSubSupport;
import com.vtc.openapi.infra.adapter.taskcenter.TaskCenterDispatchRetryResult;
import com.vtc.openapi.infra.adapter.taskcenter.TaskCenterSurveyRefetchService;
import com.vtc.openapi.infra.adapter.taskcenter.TaskCenterTaskOrchestrator;
import com.vtc.openapi.infra.config.OpenApiProperties;
import com.vtc.openapi.infra.converter.InstanceItemConverter;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.OpenTaskDispatchRetryResultDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskAdminDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskAdminPageDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskInstanceBriefDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskReportRefetchResultDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskSubDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskSurveyRefetchResultDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskSurveyResultsDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskTimelineEventDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskWorkspaceDto;
import com.vtc.openapi.ui.dto.admin.WebhookDeliveryLogDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OpenTaskAdminAppServiceImpl implements IOpenTaskAdminAppService {

    private static final Logger log = LoggerFactory.getLogger(OpenTaskAdminAppServiceImpl.class);

    private static final DateTimeFormatter ISO_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private final IOpenTaskRepository openTaskRepository;
    private final IOpenTaskSubRepository openTaskSubRepository;
    private final IOpenVulnInstanceRepository vulnInstanceRepository;
    private final IApiInvocationRepository apiInvocationRepository;
    private final AdminGovernanceAppConvertor adminGovernanceAppConvertor;
    private final OpenApiProperties openApiProperties;
    private final TaskCenterTaskOrchestrator taskCenterOrchestrator;
    private final TaskCenterScanResultQueryService scanResultQueryService;
    private final TaskCenterSurveyRefetchService surveyRefetchService;
    private final TaskCenterReportArchiveService reportArchiveService;
    private final IExportDownloadPolicy exportDownloadPolicy;

    public OpenTaskAdminAppServiceImpl(IOpenTaskRepository openTaskRepository,
                                       IOpenTaskSubRepository openTaskSubRepository,
                                       IOpenVulnInstanceRepository vulnInstanceRepository,
                                       IApiInvocationRepository apiInvocationRepository,
                                       AdminGovernanceAppConvertor adminGovernanceAppConvertor,
                                       OpenApiProperties openApiProperties,
                                       @Autowired(required = false) TaskCenterTaskOrchestrator taskCenterOrchestrator,
                                       @Autowired(required = false) TaskCenterScanResultQueryService scanResultQueryService,
                                       @Autowired(required = false) TaskCenterSurveyRefetchService surveyRefetchService,
                                       @Autowired(required = false) TaskCenterReportArchiveService reportArchiveService,
                                       IExportDownloadPolicy exportDownloadPolicy) {
        this.openTaskRepository = openTaskRepository;
        this.openTaskSubRepository = openTaskSubRepository;
        this.vulnInstanceRepository = vulnInstanceRepository;
        this.apiInvocationRepository = apiInvocationRepository;
        this.adminGovernanceAppConvertor = adminGovernanceAppConvertor;
        this.openApiProperties = openApiProperties;
        this.taskCenterOrchestrator = taskCenterOrchestrator;
        this.scanResultQueryService = scanResultQueryService;
        this.surveyRefetchService = surveyRefetchService;
        this.reportArchiveService = reportArchiveService;
        this.exportDownloadPolicy = exportDownloadPolicy;
    }

    @Override
    public ApiResponse<OpenTaskAdminPageDto> listTasks(String partnerId, String taskId, String extTaskId,
                                                       String status, Integer scanTemplateId, Integer vulnType,
                                                       int page, int size) {
        if (page < 1) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "page 必须从 1 开始");
        }
        if (size < 1 || size > 200) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "size 必须在 1-200 之间");
        }
        OpenTaskAdminQuery query = new OpenTaskAdminQuery();
        query.setPartnerId(partnerId);
        query.setTaskId(taskId);
        query.setExtTaskId(extTaskId);
        query.setStatus(status);
        query.setScanTemplateId(scanTemplateId);
        query.setVulnType(vulnType);
        query.setPage(page);
        query.setSize(size);
        PageInfo<OpenTaskDO> pageResult = openTaskRepository.pageForAdmin(query);
        OpenTaskAdminPageDto dto = new OpenTaskAdminPageDto();
        dto.setPage(page);
        dto.setSize(size);
        dto.setTotal(pageResult.getTotal());
        if (!CollectionUtils.isEmpty(pageResult.getRecords())) {
            dto.setItems(pageResult.getRecords().stream()
                    .map(this::toAdminDto)
                    .collect(Collectors.toList()));
        }
        return ApiResponse.ok(dto);
    }

    @Override
    public ApiResponse<OpenTaskWorkspaceDto> getWorkspace(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "taskId 不能为空");
        }
        OpenTaskDO task = openTaskRepository.findByTaskId(taskId);
        if (task == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "任务不存在");
        }
        List<OpenTaskSubDO> subs = openTaskSubRepository.listByTaskId(taskId);
        List<OpenVulnInstanceDO> instances = vulnInstanceRepository.listByPartnerAndTask(
                task.getPartnerId(), taskId, task.getExtTaskId());

        OpenTaskWorkspaceDto workspace = new OpenTaskWorkspaceDto();
        workspace.setTask(toAdminDto(task, subs));
        workspace.setTargetHosts(extractHosts(task));
        workspace.setSurveySubs(filterSubs(subs, TaskCenterSubSupport.PHASE_SURVEY));
        workspace.setVerifySubs(filterSubs(subs, TaskCenterSubSupport.PHASE_VERIFY));
        workspace.setInstanceStatCounts(buildStatCounts(instances));
        workspace.setInstances(buildInstanceBriefs(instances, 50));
        workspace.setWebhookDeliveries(loadWebhookDeliveries(task));
        workspace.setTimeline(buildTimeline(task, subs, instances));
        return ApiResponse.ok(workspace);
    }

    @Override
    public ApiResponse<OpenTaskSurveyResultsDto> getSurveyResults(String taskId, Integer scanPhase, String subId) {
        if (!StringUtils.hasText(taskId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "taskId 不能为空");
        }
        OpenTaskDO task = openTaskRepository.findByTaskId(taskId);
        if (task == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "任务不存在");
        }
        int phase = scanPhase != null ? scanPhase : TaskCenterSubSupport.PHASE_SURVEY;
        OpenTaskSubDO sub = resolveSurveySub(taskId, phase, subId);
        OpenTaskSurveyResultsDto dto = new OpenTaskSurveyResultsDto();
        dto.setTaskId(taskId);
        dto.setScanPhase(phase);
        if (sub == null) {
            dto.setSource("unavailable");
            dto.setHint("该阶段暂无子任务，请先下发扫描或等待编排");
            return ApiResponse.ok(dto);
        }
        dto.setSubId(sub.getSubId());
        dto.setSurveyId(sub.getSurveyId());
        dto.setScannerLabel(resolveScannerLabel(sub.getScannerType()));
        if (!StringUtils.hasText(sub.getSurveyId())) {
            dto.setSource("pending");
            dto.setHint("surveyId 尚未生成，扫描进行中");
            return ApiResponse.ok(dto);
        }
        if (scanResultQueryService == null) {
            dto.setSource("mock");
            dto.setHint("当前为 mock 模式，无 VTC 扫描结果；请查看「漏洞实例」Tab 或导入 XML");
            return ApiResponse.ok(dto);
        }
        if (!scanResultQueryService.hasPersistedResults(sub.getSubId())) {
            dto.setSource("pending");
            dto.setHint("扫描结果尚未落库，请等待任务完成通知（Kafka）后刷新");
            return ApiResponse.ok(dto);
        }
        List<Map<String, Object>> liveRows = scanResultQueryService.listLiveExportRowsBySub(sub.getSubId());
        List<Map<String, Object>> portRows = scanResultQueryService.listPortExportRowsBySub(sub.getSubId());
        dto.setSource("persisted");
        dto.setSuccessIps(scanResultQueryService.listSuccessIpsFromLiveRows(liveRows));
        dto.setFailIps(scanResultQueryService.listFailIpsFromLiveRows(liveRows));
        dto.setLiveProbeResults(liveRows);
        dto.setPortScanResults(scanResultQueryService.toVtcPortScanRows(portRows));
        dto.setVulnerabilities(scanResultQueryService.listVulnScanRowsBySub(sub.getSubId()));
        dto.setVulnDatabaseList(scanResultQueryService.listVulnDatabaseListBySub(sub.getSubId()));
        return ApiResponse.ok(dto);
    }

    @Override
    public ApiResponse<OpenTaskSurveyRefetchResultDto> refetchSurveyResults(String taskId, String subId) {
        if (!StringUtils.hasText(taskId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "taskId 不能为空");
        }
        if (!StringUtils.hasText(subId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "subId 不能为空");
        }
        if (!"task-center".equalsIgnoreCase(openApiProperties.getEngine().getAdapterMode())) {
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID, "仅 task-center 模式支持重新获取扫描结果");
        }
        if (surveyRefetchService == null) {
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID, "task-center 扫描结果回收未启用");
        }
        return ApiResponse.ok(surveyRefetchService.refetchSurveySub(taskId.trim(), subId.trim()));
    }

    @Override
    public ApiResponse<OpenTaskReportRefetchResultDto> refetchSubReport(String taskId, String subId) {
        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(subId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "taskId/subId 不能为空");
        }
        if (!"task-center".equalsIgnoreCase(openApiProperties.getEngine().getAdapterMode())) {
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID, "仅 task-center 模式支持重新获取报告");
        }
        if (reportArchiveService == null) {
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID, "task-center 报告归档未启用");
        }
        OpenTaskSubDO sub = openTaskSubRepository.findBySubId(subId.trim());
        if (sub == null || !taskId.trim().equals(sub.getTaskId())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "子任务不存在或不属于该任务");
        }
        OpenTaskReportRefetchResultDto result = new OpenTaskReportRefetchResultDto();
        result.setTaskId(taskId.trim());
        result.setSubId(sub.getSubId());
        if (!TaskCenterSubSupport.STATUS_FINISHED.equals(sub.getStatus())) {
            result.setSuccess(false);
            result.setReportArchiveStatus(sub.getReportArchiveStatus());
            result.setMessage("子任务未完成，无法重新获取报告");
            return ApiResponse.ok(result);
        }
        if (!StringUtils.hasText(sub.getReportDownloadPath())) {
            result.setSuccess(false);
            result.setReportArchiveStatus(sub.getReportArchiveStatus());
            result.setMessage("暂无报告下载路径，需等待 vuln-task-center 推送 download_report_finish");
            return ApiResponse.ok(result);
        }
        boolean ok = reportArchiveService.retryArchive(sub);
        OpenTaskSubDO latest = openTaskSubRepository.findBySubId(sub.getSubId());
        result.setSuccess(ok);
        result.setReportArchiveStatus(latest != null ? latest.getReportArchiveStatus() : sub.getReportArchiveStatus());
        result.setMessage(ok ? "报告已重新归档" : "报告归档失败，请查看失败原因或稍后重试");
        return ApiResponse.ok(result);
    }

    @Override
    public ApiResponse<OpenTaskReportRefetchResultDto> refetchAllReports(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "taskId 不能为空");
        }
        if (!"task-center".equalsIgnoreCase(openApiProperties.getEngine().getAdapterMode())) {
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID, "仅 task-center 模式支持重新获取报告");
        }
        if (reportArchiveService == null) {
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID, "task-center 报告归档未启用");
        }
        List<OpenTaskSubDO> subs = openTaskSubRepository.listByTaskId(taskId.trim());
        OpenTaskReportRefetchResultDto result = new OpenTaskReportRefetchResultDto();
        result.setTaskId(taskId.trim());
        int attempted = 0;
        int archived = 0;
        List<String> failedSubIds = new ArrayList<>();
        for (OpenTaskSubDO sub : subs) {
            // 仅对已完成、有报告路径且未成功归档的 vuln 子任务重试
            if (!TaskCenterSubSupport.STATUS_FINISHED.equals(sub.getStatus())) {
                continue;
            }
            if (!StringUtils.hasText(sub.getReportDownloadPath())) {
                continue;
            }
            if (TaskCenterSubSupport.REPORT_ARCHIVED.equals(sub.getReportArchiveStatus())) {
                continue;
            }
            attempted++;
            boolean ok = reportArchiveService.retryArchive(sub);
            if (ok) {
                archived++;
            } else {
                failedSubIds.add(sub.getSubId());
            }
        }
        result.setAttempted(attempted);
        result.setArchived(archived);
        result.setFailedSubIds(failedSubIds);
        result.setSuccess(attempted == 0 || archived > 0);
        result.setMessage(attempted == 0
                ? "没有需要重新获取报告的子任务"
                : (archived + "/" + attempted + " 个子任务报告归档成功"));
        return ApiResponse.ok(result);
    }

    @Override
    public ApiResponse<OpenTaskDispatchRetryResultDto> retrySurveyDispatch(String taskId,
                                                                           Integer scanPhase,
                                                                           String subId) {
        if (!StringUtils.hasText(taskId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "taskId 不能为空");
        }
        if (!"task-center".equalsIgnoreCase(openApiProperties.getEngine().getAdapterMode())) {
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID, "仅 task-center 模式支持手动重试下发");
        }
        if (taskCenterOrchestrator == null) {
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID, "task-center 编排未启用");
        }
        int phase = scanPhase != null ? scanPhase : TaskCenterSubSupport.PHASE_SURVEY;
        if (phase != TaskCenterSubSupport.PHASE_SURVEY && phase != TaskCenterSubSupport.PHASE_VERIFY) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "scanPhase 仅支持 1（排查）或 2（验证）");
        }
        try {
            TaskCenterDispatchRetryResult retry = taskCenterOrchestrator.retryDispatchManual(taskId, phase, subId);
            return ApiResponse.ok(toDispatchRetryDto(retry));
        } catch (Exception ex) {
            log.error("retrySurveyDispatch failed taskId={} scanPhase={} subId={}", taskId, phase, subId, ex);
            OpenTaskDispatchRetryResultDto dto = new OpenTaskDispatchRetryResultDto();
            dto.setTaskId(taskId);
            dto.setSuccess(false);
            dto.setMessage("重试下发失败，请稍后重试或联系平台运维");
            dto.setRetriedCount(0);
            dto.setSuccessCount(0);
            dto.setFailedCount(0);
            return ApiResponse.ok(dto);
        }
    }

    private static OpenTaskDispatchRetryResultDto toDispatchRetryDto(TaskCenterDispatchRetryResult retry) {
        OpenTaskDispatchRetryResultDto dto = new OpenTaskDispatchRetryResultDto();
        dto.setTaskId(retry.getTaskId());
        dto.setSuccess(retry.isSuccess());
        dto.setMessage(retry.getMessage());
        dto.setTaskStatus(retry.getTaskStatus());
        dto.setRetriedCount(retry.getRetriedCount());
        dto.setSuccessCount(retry.getSuccessCount());
        dto.setFailedCount(retry.getFailedCount());
        return dto;
    }

    private OpenTaskSubDO resolveSurveySub(String taskId, int phase, String subId) {
        List<OpenTaskSubDO> subs = openTaskSubRepository.listByTaskIdAndPhase(taskId, phase);
        if (CollectionUtils.isEmpty(subs)) {
            return null;
        }
        if (StringUtils.hasText(subId)) {
            for (OpenTaskSubDO sub : subs) {
                if (subId.equals(sub.getSubId())) {
                    return sub;
                }
            }
            return null;
        }
        for (OpenTaskSubDO sub : subs) {
            if (StringUtils.hasText(sub.getSurveyId())) {
                return sub;
            }
        }
        return subs.get(0);
    }

    private OpenTaskAdminDto toAdminDto(OpenTaskDO task) {
        List<OpenTaskSubDO> subs = openTaskSubRepository.listByTaskId(task.getTaskId());
        return toAdminDto(task, subs);
    }

    private OpenTaskAdminDto toAdminDto(OpenTaskDO task, List<OpenTaskSubDO> subs) {
        OpenTaskAdminDto dto = new OpenTaskAdminDto();
        dto.setTaskId(task.getTaskId());
        dto.setCaseId(task.getCaseId());
        dto.setExtTaskId(task.getExtTaskId());
        dto.setPartnerId(task.getPartnerId());
        dto.setTaskName(task.getTaskName());
        dto.setVulnType(task.getVulnType());
        dto.setScanTemplateId(task.getScanTemplateId());
        dto.setAutoVerify(task.getAutoVerify());
        dto.setCrossScan(task.getCrossScan());
        dto.setVerifyMergeStrategy(task.getVerifyMergeStrategy());
        dto.setTaskPhase(task.getTaskPhase());
        dto.setStatus(task.getStatus());
        dto.setProgress(task.getProgress());
        dto.setAdapterMode(openApiProperties.getEngine().getAdapterMode());
        dto.setInstanceCount(vulnInstanceRepository.countByPartnerAndTaskId(
                task.getPartnerId(), task.getTaskId()));
        dto.setSubTaskCount(subs != null ? subs.size() : 0);
        dto.setCreatedAt(formatUtc(task.getCreatedAt()));
        dto.setStartedAt(formatUtc(task.getStartedAt()));
        dto.setFinishedAt(formatUtc(task.getFinishedAt()));
        return dto;
    }

    private List<OpenTaskSubDto> filterSubs(List<OpenTaskSubDO> subs, int phase) {
        if (CollectionUtils.isEmpty(subs)) {
            return Collections.emptyList();
        }
        return subs.stream()
                .filter(s -> s.getScanPhase() != null && s.getScanPhase() == phase)
                .map(this::toSubDto)
                .collect(Collectors.toList());
    }

    private OpenTaskSubDto toSubDto(OpenTaskSubDO sub) {
        OpenTaskSubDto dto = new OpenTaskSubDto();
        dto.setSubId(sub.getSubId());
        dto.setScanPhase(sub.getScanPhase());
        dto.setScannerType(sub.getScannerType());
        dto.setScannerLabel(resolveScannerLabel(sub.getScannerType()));
        dto.setCenterTaskType(sub.getCenterTaskType());
        dto.setCenterPlanId(sub.getCenterPlanId());
        dto.setSurveyId(sub.getSurveyId());
        dto.setStatus(sub.getStatus());
        dto.setProgress(sub.getProgress());
        dto.setErrorMessage(sub.getErrorMessage());
        dto.setReportDownloadPath(sub.getReportDownloadPath());
        dto.setReportArchiveStatus(sub.getReportArchiveStatus());
        dto.setReportArchiveError(sub.getReportArchiveError());
        dto.setCreatedAt(formatUtc(sub.getCreatedAt()));
        dto.setUpdatedAt(formatUtc(sub.getUpdatedAt()));
        return dto;
    }

    private static String resolveScannerLabel(String scannerType) {
        if ("1".equals(scannerType)) {
            return "绿盟 RSAS";
        }
        if ("7".equals(scannerType)) {
            return "Nessus";
        }
        return scannerType != null ? "scanner-" + scannerType : "-";
    }

    private static String extractHosts(OpenTaskDO task) {
        if (!StringUtils.hasText(task.getTargetsJson())) {
            return null;
        }
        JSONObject json = JSON.parseObject(task.getTargetsJson());
        return json != null ? json.getString("hosts") : null;
    }

    private static Map<String, Long> buildStatCounts(List<OpenVulnInstanceDO> instances) {
        if (CollectionUtils.isEmpty(instances)) {
            return Collections.emptyMap();
        }
        Map<String, Long> counts = new LinkedHashMap<>();
        for (OpenVulnInstanceDO row : instances) {
            String key = row.getVulInfoStat() != null ? String.valueOf(row.getVulInfoStat()) : "unknown";
            counts.merge(key, 1L, Long::sum);
        }
        return counts;
    }

    private static List<OpenTaskInstanceBriefDto> buildInstanceBriefs(List<OpenVulnInstanceDO> instances, int limit) {
        if (CollectionUtils.isEmpty(instances)) {
            return Collections.emptyList();
        }
        List<OpenTaskInstanceBriefDto> result = new ArrayList<>();
        int max = Math.min(limit, instances.size());
        for (int i = 0; i < max; i++) {
            OpenVulnInstanceDO row = instances.get(i);
            InstanceItemResult item = InstanceItemConverter.fromSnapshot(row);
            OpenTaskInstanceBriefDto brief = new OpenTaskInstanceBriefDto();
            brief.setVulInfoId(row.getVulInfoId());
            brief.setVulInfoStat(row.getVulInfoStat());
            if (item != null) {
                brief.setAddress(item.getVulNetAddr());
                brief.setPort(item.getVulPort() != null ? String.valueOf(item.getVulPort()) : null);
                brief.setVulnName(item.getVulName());
                brief.setLevel(item.getVulLevel() != null ? String.valueOf(item.getVulLevel()) : null);
            }
            result.add(brief);
        }
        return result;
    }

    private List<WebhookDeliveryLogDTO> loadWebhookDeliveries(OpenTaskDO task) {
        List<WebhookDeliveryLogDO> rows = apiInvocationRepository.listByResource(
                task.getPartnerId(), OpenApiOperations.RESOURCE_TYPE_TASK, task.getTaskId(), 20);
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyList();
        }
        return rows.stream()
                .map(adminGovernanceAppConvertor::toWebhookDeliveryLogDto)
                .peek(dto -> dto.setExportDownloadable(
                        exportDownloadPolicy.isDownloadable(task.getPartnerId(), dto.getExportId())))
                .collect(Collectors.toList());
    }

    private List<OpenTaskTimelineEventDto> buildTimeline(OpenTaskDO task,
                                                         List<OpenTaskSubDO> subs,
                                                         List<OpenVulnInstanceDO> instances) {
        List<OpenTaskTimelineEventDto> events = new ArrayList<>();
        events.add(timeline("Partner 创建任务", task.getCreatedAt(), "done"));
        if (task.getStartedAt() != null || "RUNNING".equals(task.getStatus())
                || "FINISHED".equals(task.getStatus()) || "FAILED".equals(task.getStatus())) {
            events.add(timeline("引擎下发 / 编排启动", firstNonNull(task.getStartedAt(), task.getCreatedAt()), "done"));
        }
        boolean surveyDone = subs != null && subs.stream()
                .anyMatch(s -> s.getScanPhase() != null && s.getScanPhase() == TaskCenterSubSupport.PHASE_SURVEY
                        && TaskCenterSubSupport.STATUS_FINISHED.equals(s.getStatus()));
        if (surveyDone || !CollectionUtils.isEmpty(instances)) {
            String mergeHint = Boolean.TRUE.equals(task.getCrossScan())
                    ? " · 双扫交叉合并"
                    : "";
            events.add(timeline("排查阶段完成 · 实例入库 " + (instances != null ? instances.size() : 0) + " 条" + mergeHint,
                    task.getUpdatedAt(), "FINISHED".equals(task.getStatus()) ? "done" : "active"));
        }
        if (Boolean.TRUE.equals(task.getCrossScan())) {
            if ("FINISHED".equals(task.getStatus())) {
                events.add(timeline("交叉合并完成（基于排查双扫结果，无二次下发）", task.getFinishedAt(), "done"));
            } else if (surveyDone) {
                events.add(timeline("交叉合并处理中", null, "active"));
            }
        } else if (Boolean.TRUE.equals(task.getAutoVerify())) {
            boolean verifyRunning = subs != null && subs.stream()
                    .anyMatch(s -> s.getScanPhase() != null && s.getScanPhase() == TaskCenterSubSupport.PHASE_VERIFY
                            && TaskCenterSubSupport.STATUS_RUNNING.equals(s.getStatus()));
            boolean verifyDone = subs != null && subs.stream()
                    .anyMatch(s -> s.getScanPhase() != null && s.getScanPhase() == TaskCenterSubSupport.PHASE_VERIFY)
                    && subs.stream()
                    .filter(s -> s.getScanPhase() != null && s.getScanPhase() == TaskCenterSubSupport.PHASE_VERIFY)
                    .allMatch(s -> TaskCenterSubSupport.STATUS_FINISHED.equals(s.getStatus())
                            || TaskCenterSubSupport.STATUS_FAILED.equals(s.getStatus()));
            if (verifyDone) {
                events.add(timeline("验证阶段完成 · mergeVerifyResults", task.getFinishedAt(), "done"));
            } else if (verifyRunning || Integer.valueOf(TaskCenterSubSupport.PHASE_VERIFY).equals(task.getTaskPhase())) {
                events.add(timeline("验证阶段进行中", null, "active"));
            } else {
                events.add(timeline("待触发验证阶段（autoVerify）", null, "pending"));
            }
        }
        if ("FINISHED".equals(task.getStatus())) {
            events.add(timeline("TASK_COMPLETED + EXPORT_READY 回调", task.getFinishedAt(), "done"));
        } else if ("FAILED".equals(task.getStatus())) {
            events.add(timeline("TASK_FAILED 回调", task.getFinishedAt(), "done"));
        } else if (Boolean.TRUE.equals(task.getAutoVerify()) && !Boolean.TRUE.equals(task.getCrossScan())) {
            events.add(timeline("推迟回调：验证全部完成后统一触发", null, "pending"));
        } else {
            events.add(timeline("待任务完成后回调 Partner", null, "pending"));
        }
        return events;
    }

    private static OpenTaskTimelineEventDto timeline(String label, Date at, String state) {
        OpenTaskTimelineEventDto event = new OpenTaskTimelineEventDto();
        event.setLabel(label);
        event.setAt(formatUtcStatic(at));
        event.setState(state);
        return event;
    }

    private static Date firstNonNull(Date a, Date b) {
        return a != null ? a : b;
    }

    private String formatUtc(Date date) {
        return formatUtcStatic(date);
    }

    private static String formatUtcStatic(Date date) {
        if (date == null) {
            return null;
        }
        return ISO_UTC.format(date.toInstant());
    }
}
