package com.vtc.openapi.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vtc.openapi.adapter.SvmpEngineAdapter;
import com.vtc.openapi.adapter.dto.SvmpTaskCreateRequest;
import com.vtc.openapi.adapter.dto.SvmpTaskCreateResult;
import com.vtc.openapi.adapter.dto.SvmpTaskProgressResult;
import com.vtc.openapi.app.service.IOpenTaskAppService;
import com.vtc.openapi.common.OpenApiConstants;
import com.vtc.openapi.common.OpenApiException;
import com.vtc.openapi.common.PartnerContext;
import com.vtc.openapi.infra.dao.OpenTaskMapper;
import com.vtc.openapi.infra.dao.PartnerTaskMapMapper;
import com.vtc.openapi.infra.dao.po.OpenTaskPO;
import com.vtc.openapi.infra.dao.po.PartnerTaskMapPO;
import com.vtc.openapi.service.TaskService;
import com.vtc.openapi.web.dto.ApiResponse;
import com.vtc.openapi.web.dto.task.CreateTaskRequest;
import com.vtc.openapi.web.dto.task.CreateTaskResponse;
import com.vtc.openapi.web.dto.task.TaskListPageDto;
import com.vtc.openapi.web.dto.task.TaskProgressDto;
import com.vtc.openapi.web.dto.task.TaskSummaryDto;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService, IOpenTaskAppService {

    private static final String ACCEPT_STATUS = "ACCEPTED";
    private static final SimpleDateFormat ISO_UTC;

    static {
        ISO_UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        ISO_UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final OpenTaskMapper openTaskMapper;
    private final PartnerTaskMapMapper partnerTaskMapMapper;
    private final SvmpEngineAdapter svmpEngineAdapter;

    public TaskServiceImpl(OpenTaskMapper openTaskMapper,
                           PartnerTaskMapMapper partnerTaskMapMapper,
                           SvmpEngineAdapter svmpEngineAdapter) {
        this.openTaskMapper = openTaskMapper;
        this.partnerTaskMapMapper = partnerTaskMapMapper;
        this.svmpEngineAdapter = svmpEngineAdapter;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<CreateTaskResponse> createTask(CreateTaskRequest request) {
        String partnerId = PartnerContext.requirePartnerId();
        PartnerTaskMapPO existingMap = partnerTaskMapMapper.selectOne(
                new LambdaQueryWrapper<PartnerTaskMapPO>()
                        .eq(PartnerTaskMapPO::getPartnerId, partnerId)
                        .eq(PartnerTaskMapPO::getExtTaskId, request.getExtTaskId()));
        if (existingMap != null) {
            OpenTaskPO existingTask = openTaskMapper.selectOne(
                    new LambdaQueryWrapper<OpenTaskPO>()
                            .eq(OpenTaskPO::getTaskId, existingMap.getPlatformTaskId()));
            return ApiResponse.of(OpenApiConstants.CODE_IDEMPOTENT_CONFLICT, "extTaskId 已存在",
                    toCreateResponse(existingTask, request.getExtTaskId()));
        }

        SvmpTaskCreateRequest engineReq = toEngineRequest(request);
        SvmpTaskCreateResult engineResult = svmpEngineAdapter.createTask(engineReq);

        String platformTaskId = "TASK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Date now = new Date();

        OpenTaskPO task = new OpenTaskPO();
        task.setTaskId(platformTaskId);
        task.setPartnerId(partnerId);
        task.setExtTaskId(request.getExtTaskId());
        task.setEngineTaskId(engineResult.getEngineTaskId());
        task.setTaskName(request.getTaskName());
        task.setTargetType(request.getTargetType());
        task.setVulnType(request.getVulnType());
        task.setTargetsJson(JSON.toJSONString(request.getTargets()));
        task.setStatus(ACCEPT_STATUS);
        task.setProgress(0);
        task.setScanTemplateId(request.getScanTemplateId());
        task.setCallbackUrl(request.getCallbackUrl());
        if (request.getOptions() != null) {
            task.setOptionsJson(JSON.toJSONString(request.getOptions()));
        }
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        PartnerTaskMapPO map = new PartnerTaskMapPO();
        map.setPartnerId(partnerId);
        map.setExtTaskId(request.getExtTaskId());
        map.setPlatformTaskId(platformTaskId);
        map.setCreatedAt(now);

        try {
            openTaskMapper.insert(task);
            partnerTaskMapMapper.insert(map);
        } catch (DuplicateKeyException ex) {
            PartnerTaskMapPO raced = partnerTaskMapMapper.selectOne(
                    new LambdaQueryWrapper<PartnerTaskMapPO>()
                            .eq(PartnerTaskMapPO::getPartnerId, partnerId)
                            .eq(PartnerTaskMapPO::getExtTaskId, request.getExtTaskId()));
            OpenTaskPO racedTask = openTaskMapper.selectOne(
                    new LambdaQueryWrapper<OpenTaskPO>()
                            .eq(OpenTaskPO::getTaskId, raced.getPlatformTaskId()));
            return ApiResponse.of(OpenApiConstants.CODE_IDEMPOTENT_CONFLICT, "extTaskId 已存在",
                    toCreateResponse(racedTask, request.getExtTaskId()));
        }

        return ApiResponse.ok(toCreateResponse(task, request.getExtTaskId()));
    }

    @Override
    public ApiResponse<TaskProgressDto> getTask(String taskId) {
        String partnerId = PartnerContext.requirePartnerId();
        OpenTaskPO task = requireOwnedTask(taskId, partnerId);
        mergeEngineProgress(task);
        return ApiResponse.ok(toProgressDto(task));
    }

    @Override
    public ApiResponse<TaskListPageDto> listTasks(String extTaskId, String status,
                                                  String createdFrom, String createdTo,
                                                  int page, int size) {
        String partnerId = PartnerContext.requirePartnerId();
        if (page < 1) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "page 必须从 1 开始");
        }
        if (size < 1 || size > 1000) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "size 必须在 1-1000 之间");
        }

        LambdaQueryWrapper<OpenTaskPO> wrapper = new LambdaQueryWrapper<OpenTaskPO>()
                .eq(OpenTaskPO::getPartnerId, partnerId)
                .orderByDesc(OpenTaskPO::getCreatedAt);
        if (StringUtils.hasText(extTaskId)) {
            wrapper.eq(OpenTaskPO::getExtTaskId, extTaskId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(OpenTaskPO::getStatus, status);
        }
        Date from = parseDate(createdFrom);
        Date to = parseDate(createdTo);
        if (from != null) {
            wrapper.ge(OpenTaskPO::getCreatedAt, from);
        }
        if (to != null) {
            wrapper.le(OpenTaskPO::getCreatedAt, to);
        }

        Page<OpenTaskPO> pageResult = openTaskMapper.selectPage(new Page<>(page, size), wrapper);
        TaskListPageDto data = new TaskListPageDto();
        data.setPage(page);
        data.setSize(size);
        data.setTotal(pageResult.getTotal());
        data.setItems(pageResult.getRecords().stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList()));
        return ApiResponse.ok(data);
    }

    private OpenTaskPO requireOwnedTask(String taskId, String partnerId) {
        OpenTaskPO task = openTaskMapper.selectOne(
                new LambdaQueryWrapper<OpenTaskPO>().eq(OpenTaskPO::getTaskId, taskId));
        if (task == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "任务不存在");
        }
        if (!partnerId.equals(task.getPartnerId())) {
            throw new OpenApiException(OpenApiConstants.CODE_CROSS_PARTNER, "无权访问该任务");
        }
        return task;
    }

    private void mergeEngineProgress(OpenTaskPO task) {
        if (!StringUtils.hasText(task.getEngineTaskId())) {
            return;
        }
        SvmpTaskProgressResult progress = svmpEngineAdapter.getTaskProgress(task.getEngineTaskId());
        task.setStatus(progress.getStatus());
        task.setProgress(progress.getProgress());
        task.setErrorMessage(progress.getErrorMessage());
        if ("RUNNING".equals(progress.getStatus()) && task.getStartedAt() == null) {
            task.setStartedAt(new Date());
        }
        if ("FINISHED".equals(progress.getStatus()) || "FAILED".equals(progress.getStatus())) {
            task.setFinishedAt(new Date());
        }
        task.setUpdatedAt(new Date());
        openTaskMapper.updateById(task);
    }

    private SvmpTaskCreateRequest toEngineRequest(CreateTaskRequest request) {
        SvmpTaskCreateRequest engineReq = new SvmpTaskCreateRequest();
        engineReq.setTaskName(request.getTaskName());
        engineReq.setTargets(request.getTargets());
        engineReq.setTargetType(request.getTargetType());
        engineReq.setVulnType(request.getVulnType());
        engineReq.setScanTemplateId(request.getScanTemplateId());
        engineReq.setPriority(request.getPriority());
        engineReq.setOptions(request.getOptions());
        return engineReq;
    }

    private CreateTaskResponse toCreateResponse(OpenTaskPO task, String extTaskId) {
        CreateTaskResponse resp = new CreateTaskResponse();
        resp.setExtTaskId(extTaskId);
        resp.setTaskId(task.getTaskId());
        resp.setStatus(task.getStatus() != null ? task.getStatus() : ACCEPT_STATUS);
        resp.setCreatedAt(formatUtc(task.getCreatedAt()));
        return resp;
    }

    private TaskProgressDto toProgressDto(OpenTaskPO task) {
        TaskProgressDto dto = new TaskProgressDto();
        dto.setExtTaskId(task.getExtTaskId());
        dto.setTaskId(task.getTaskId());
        dto.setStatus(task.getStatus());
        dto.setProgress(task.getProgress());
        dto.setStartedAt(formatUtc(task.getStartedAt()));
        dto.setFinishedAt(formatUtc(task.getFinishedAt()));
        dto.setErrorMessage(task.getErrorMessage());
        return dto;
    }

    private TaskSummaryDto toSummaryDto(OpenTaskPO task) {
        TaskSummaryDto dto = new TaskSummaryDto();
        dto.setExtTaskId(task.getExtTaskId());
        dto.setTaskId(task.getTaskId());
        dto.setTaskName(task.getTaskName());
        dto.setStatus(task.getStatus());
        dto.setProgress(task.getProgress());
        dto.setStartedAt(formatUtc(task.getStartedAt()));
        dto.setFinishedAt(formatUtc(task.getFinishedAt()));
        dto.setErrorMessage(task.getErrorMessage());
        dto.setCreatedAt(formatUtc(task.getCreatedAt()));
        return dto;
    }

    private String formatUtc(Date date) {
        if (date == null) {
            return null;
        }
        synchronized (ISO_UTC) {
            return ISO_UTC.format(date);
        }
    }

    private Date parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            synchronized (ISO_UTC) {
                return ISO_UTC.parse(value);
            }
        } catch (ParseException e) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "日期格式无效，需 ISO 8601 UTC");
        }
    }
}
