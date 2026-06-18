package com.vtc.openapi.app.service.impl;

import com.vtc.openapi.app.convert.AdminGovernanceAppConvertor;
import com.vtc.openapi.app.convert.VerifyFixJobAdminConvertor;
import com.vtc.openapi.app.support.OpenTaskSubAdminMapper;
import com.vtc.openapi.domain.export.model.ExportStage;
import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.export.repository.IOpenExportRepository;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobItemDO;
import com.vtc.openapi.domain.instance.repository.IOpenVerifyFixJobRepository;
import com.vtc.openapi.domain.instance.service.business.IVerifyFixJobDomainService;
import com.vtc.openapi.domain.open.OpenApiOperations;
import com.vtc.openapi.domain.open.model.entity.WebhookDeliveryLogDO;
import com.vtc.openapi.domain.open.repository.IApiInvocationRepository;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.domain.task.repository.IOpenTaskSubRepository;
import com.vtc.openapi.infra.adapter.taskcenter.TaskCenterSubSupport;
import com.vtc.openapi.infra.config.OpenApiProperties;
import com.vtc.openapi.ui.dto.admin.MockVerifyFixJobDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskAdminDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskSubDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskTimelineEventDto;
import com.vtc.openapi.ui.dto.admin.VerifyFixExportBriefDto;
import com.vtc.openapi.ui.dto.admin.VerifyFixWorkspaceDto;
import com.vtc.openapi.ui.dto.admin.WebhookDeliveryLogDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

@Component
public class VerifyFixWorkspaceAssembler {

    private static final SimpleDateFormat ISO_UTC;

    static {
        ISO_UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        ISO_UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final IOpenVerifyFixJobRepository verifyFixJobRepository;
    private final IOpenTaskSubRepository openTaskSubRepository;
    private final IOpenTaskRepository openTaskRepository;
    private final IApiInvocationRepository apiInvocationRepository;
    private final IOpenExportRepository exportRepository;
    private final VerifyFixJobAdminConvertor verifyFixJobAdminConvertor;
    private final AdminGovernanceAppConvertor adminGovernanceAppConvertor;
    private final OpenTaskSubAdminMapper openTaskSubAdminMapper;
    private final OpenApiProperties openApiProperties;

    public VerifyFixWorkspaceAssembler(IOpenVerifyFixJobRepository verifyFixJobRepository,
                                       IOpenTaskSubRepository openTaskSubRepository,
                                       IOpenTaskRepository openTaskRepository,
                                       IApiInvocationRepository apiInvocationRepository,
                                       IOpenExportRepository exportRepository,
                                       VerifyFixJobAdminConvertor verifyFixJobAdminConvertor,
                                       AdminGovernanceAppConvertor adminGovernanceAppConvertor,
                                       OpenTaskSubAdminMapper openTaskSubAdminMapper,
                                       OpenApiProperties openApiProperties) {
        this.verifyFixJobRepository = verifyFixJobRepository;
        this.openTaskSubRepository = openTaskSubRepository;
        this.openTaskRepository = openTaskRepository;
        this.apiInvocationRepository = apiInvocationRepository;
        this.exportRepository = exportRepository;
        this.verifyFixJobAdminConvertor = verifyFixJobAdminConvertor;
        this.adminGovernanceAppConvertor = adminGovernanceAppConvertor;
        this.openTaskSubAdminMapper = openTaskSubAdminMapper;
        this.openApiProperties = openApiProperties;
    }

    public VerifyFixWorkspaceDto build(String jobId) {
        OpenVerifyFixJobDO job = verifyFixJobRepository.findByJobId(jobId);
        if (job == null) {
            return null;
        }
        List<OpenVerifyFixJobItemDO> items = verifyFixJobRepository.listItemsByJobId(jobId);
        List<OpenTaskSubDO> subs = openTaskSubRepository.listByVerifyFixJobId(jobId);

        VerifyFixWorkspaceDto workspace = new VerifyFixWorkspaceDto();
        MockVerifyFixJobDto jobDto = verifyFixJobAdminConvertor.toJobDto(job, true);
        jobDto.setRescanSubCount(subs != null ? subs.size() : 0);
        workspace.setJob(jobDto);

        List<OpenTaskSubDto> rescanSubs = new ArrayList<>();
        if (!CollectionUtils.isEmpty(subs)) {
            for (OpenTaskSubDO sub : subs) {
                rescanSubs.add(openTaskSubAdminMapper.toDto(sub));
            }
        }
        workspace.setRescanSubs(rescanSubs);
        workspace.setItemStatCounts(buildItemStatCounts(items));
        workspace.setItemResultCounts(buildItemResultCounts(items));
        workspace.setRelatedTasks(buildRelatedTasks(items));
        workspace.setExports(loadExports(job, items));
        workspace.setWebhookDeliveries(loadWebhookDeliveries(job));
        workspace.setTimeline(buildTimeline(job, subs, items));
        workspace.setConstraints(buildConstraints());
        return workspace;
    }

    public MockVerifyFixJobDto toListDto(OpenVerifyFixJobDO job) {
        if (job == null) {
            return null;
        }
        MockVerifyFixJobDto dto = verifyFixJobAdminConvertor.toJobDto(job, false);
        List<OpenTaskSubDO> subs = openTaskSubRepository.listByVerifyFixJobId(job.getJobId());
        dto.setRescanSubCount(subs != null ? subs.size() : 0);
        return dto;
    }

    private static Map<String, Long> buildItemStatCounts(List<OpenVerifyFixJobItemDO> items) {
        if (CollectionUtils.isEmpty(items)) {
            return Collections.emptyMap();
        }
        Map<String, Long> counts = new LinkedHashMap<>();
        for (OpenVerifyFixJobItemDO item : items) {
            String key = StringUtils.hasText(item.getItemStatus()) ? item.getItemStatus() : "unknown";
            counts.merge(key, 1L, Long::sum);
        }
        return counts;
    }

    private static Map<String, Long> buildItemResultCounts(List<OpenVerifyFixJobItemDO> items) {
        if (CollectionUtils.isEmpty(items)) {
            return Collections.emptyMap();
        }
        Map<String, Long> counts = new LinkedHashMap<>();
        for (OpenVerifyFixJobItemDO item : items) {
            if (item.getResultStat() == null) {
                continue;
            }
            String key = String.valueOf(item.getResultStat());
            counts.merge(key, 1L, Long::sum);
        }
        return counts;
    }

    private List<OpenTaskAdminDto> buildRelatedTasks(List<OpenVerifyFixJobItemDO> items) {
        if (CollectionUtils.isEmpty(items)) {
            return Collections.emptyList();
        }
        Set<String> taskIds = new LinkedHashSet<>();
        for (OpenVerifyFixJobItemDO item : items) {
            if (item != null && StringUtils.hasText(item.getTaskId())) {
                taskIds.add(item.getTaskId());
            }
        }
        List<OpenTaskAdminDto> result = new ArrayList<>();
        for (String taskId : taskIds) {
            OpenTaskDO task = openTaskRepository.findByTaskId(taskId);
            if (task == null) {
                continue;
            }
            OpenTaskAdminDto dto = new OpenTaskAdminDto();
            dto.setTaskId(task.getTaskId());
            dto.setCaseId(task.getCaseId());
            dto.setExtTaskId(task.getExtTaskId());
            dto.setPartnerId(task.getPartnerId());
            dto.setTaskName(task.getTaskName());
            dto.setStatus(task.getStatus());
            dto.setAdapterMode(openApiProperties.getEngine().getAdapterMode());
            result.add(dto);
        }
        return result;
    }

    private List<VerifyFixExportBriefDto> loadExports(OpenVerifyFixJobDO job, List<OpenVerifyFixJobItemDO> items) {
        if (job == null || CollectionUtils.isEmpty(items)) {
            return Collections.emptyList();
        }
        Set<String> taskIds = new LinkedHashSet<>();
        for (OpenVerifyFixJobItemDO item : items) {
            if (item != null && StringUtils.hasText(item.getTaskId())) {
                taskIds.add(item.getTaskId());
            }
        }
        List<VerifyFixExportBriefDto> result = new ArrayList<>();
        for (String taskId : taskIds) {
            com.botany.spore.core.page.PageInfo<OpenExportDO> page = exportRepository.pageByTask(
                    job.getPartnerId(), taskId, 1, 50);
            if (page == null || CollectionUtils.isEmpty(page.getRecords())) {
                continue;
            }
            for (OpenExportDO row : page.getRecords()) {
                if (row == null || !ExportStage.VERIFY_FIX_SCAN.equals(row.getExportStage())) {
                    continue;
                }
                if (StringUtils.hasText(row.getVerifyFixJobId())
                        && !job.getJobId().equals(row.getVerifyFixJobId())) {
                    continue;
                }
                VerifyFixExportBriefDto brief = new VerifyFixExportBriefDto();
                brief.setExportId(row.getExportId());
                brief.setTaskId(row.getTaskId());
                brief.setExportStage(row.getExportStage());
                brief.setFormat(row.getFormat());
                brief.setStatus(row.getStatus());
                brief.setDownloadUrl(row.getDownloadUrl());
                brief.setGeneratedAt(formatUtc(row.getGeneratedAt()));
                result.add(brief);
            }
        }
        return result;
    }

    private List<WebhookDeliveryLogDTO> loadWebhookDeliveries(OpenVerifyFixJobDO job) {
        List<WebhookDeliveryLogDO> rows = apiInvocationRepository.listByResource(
                job.getPartnerId(), OpenApiOperations.PRIMARY_RESOURCE_VERIFY_FIX_JOB, job.getJobId(), 30);
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyList();
        }
        List<WebhookDeliveryLogDTO> result = new ArrayList<>();
        for (WebhookDeliveryLogDO row : rows) {
            result.add(adminGovernanceAppConvertor.toWebhookDeliveryLogDto(row));
        }
        return result;
    }

    private List<OpenTaskTimelineEventDto> buildTimeline(OpenVerifyFixJobDO job,
                                                         List<OpenTaskSubDO> subs,
                                                         List<OpenVerifyFixJobItemDO> items) {
        List<OpenTaskTimelineEventDto> events = new ArrayList<>();
        events.add(timeline("Partner 受理 verify-fix · job 创建", job.getCreatedAt(), "done"));
        if (!CollectionUtils.isEmpty(subs)) {
            for (OpenTaskSubDO sub : subs) {
                String label = "复扫下发 · " + OpenTaskSubAdminMapper.resolveScannerLabel(sub.getScannerType())
                        + " · " + sub.getSubId();
                events.add(timeline(label, sub.getCreatedAt(),
                        TaskCenterSubSupport.STATUS_FINISHED.equals(sub.getStatus()) ? "done" : "active"));
                if (StringUtils.hasText(sub.getReportDownloadPath())) {
                    events.add(timeline("报告产物就绪 · " + sub.getSubId(), sub.getUpdatedAt(), "done"));
                }
            }
        } else if (IVerifyFixJobDomainService.STATUS_RUNNING.equals(job.getStatus())
                || IVerifyFixJobDomainService.STATUS_PENDING.equals(job.getStatus())) {
            events.add(timeline("等待复扫子任务下发", null, "pending"));
        }
        if (!CollectionUtils.isEmpty(items)) {
            long done = items.stream()
                    .filter(i -> IVerifyFixJobDomainService.ITEM_DONE.equals(i.getItemStatus())
                            || IVerifyFixJobDomainService.ITEM_FAILED.equals(i.getItemStatus()))
                    .count();
            events.add(timeline("逐项比对进度 " + done + "/" + items.size(), job.getUpdatedAt(),
                    done >= items.size() ? "done" : "active"));
        }
        if (IVerifyFixJobDomainService.STATUS_FINISHED.equals(job.getStatus())) {
            events.add(timeline("INSTANCE_VERIFY_FIX_COMPLETED + VERIFY_FIX_SCAN 外发", job.getFinishedAt(), "done"));
        } else if (IVerifyFixJobDomainService.STATUS_FAILED.equals(job.getStatus())) {
            events.add(timeline("修复核验失败", job.getFinishedAt(), "done"));
        } else {
            events.add(timeline("等待全部复扫 sub 完成并比对", null, "pending"));
        }
        return events;
    }

    private static List<String> buildConstraints() {
        List<String> lines = new ArrayList<>();
        lines.add("前置：实例 vulInfoStat 须为 5（已修复）");
        lines.add("扫描器：默认取最近一次 open_vuln_instance_log.sub_id 对应 open_task_sub.scanner_type");
        lines.add("下发：按 (taskId, scanner_type) 分组创建 open_task_sub(scan_phase=3)");
        lines.add("回收：task_finish_topic 指纹比对；download_report_finish_topic 写入 report_download_path");
        return lines;
    }

    private static OpenTaskTimelineEventDto timeline(String label, Date at, String state) {
        OpenTaskTimelineEventDto event = new OpenTaskTimelineEventDto();
        event.setLabel(label);
        event.setAt(formatUtc(at));
        event.setState(state);
        return event;
    }

    private static String formatUtc(Date date) {
        return date != null ? ISO_UTC.format(date) : null;
    }
}
