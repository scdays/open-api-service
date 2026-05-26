package com.vtc.openapi.app.service.impl;

import com.vtc.openapi.app.open.InvocationPipeline;
import com.vtc.openapi.app.service.IOpenTaskAppService;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.open.OpenApiOperations;
import com.vtc.openapi.domain.task.model.command.CreateOpenTaskCommand;
import com.vtc.openapi.domain.task.model.query.OpenTaskListQuery;
import com.vtc.openapi.domain.task.model.result.OpenTaskCreatedResult;
import com.vtc.openapi.domain.task.model.result.OpenTaskListResult;
import com.vtc.openapi.domain.task.model.result.OpenTaskProgressResult;
import com.vtc.openapi.domain.task.model.result.OpenTaskSummaryResult;
import com.vtc.openapi.domain.task.service.business.IOpenTaskDomainService;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.open.task.CreateTaskRequest;
import com.vtc.openapi.ui.dto.open.task.CreateTaskResponse;
import com.vtc.openapi.ui.dto.open.task.TaskListPageDto;
import com.vtc.openapi.ui.dto.open.task.TaskProgressDto;
import com.vtc.openapi.ui.dto.open.task.TaskSummaryDto;
import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * 任务应用服务：DTO 转换 + InvocationPipeline 编排。
 */
@Service
public class OpenTaskAppServiceImpl implements IOpenTaskAppService {

    private static final SimpleDateFormat ISO_UTC;

    static {
        ISO_UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        ISO_UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final InvocationPipeline invocationPipeline;
    private final IOpenTaskDomainService openTaskDomainService;
    private final Validator validator;

    public OpenTaskAppServiceImpl(InvocationPipeline invocationPipeline,
                                  IOpenTaskDomainService openTaskDomainService,
                                  Validator validator) {
        this.invocationPipeline = invocationPipeline;
        this.openTaskDomainService = openTaskDomainService;
        this.validator = validator;
    }

    @Override
    public ApiResponse<CreateTaskResponse> createTask(CreateTaskRequest request) {
        validateRequest(request);
        CreateOpenTaskCommand command = toCommand(request);
        return invocationPipeline.invoke(OpenApiOperations.CREATE_TASK, ctx -> {
            try {
                return toCreateResponse(openTaskDomainService.create(ctx, command));
            } catch (OpenApiException ex) {
                if (ex.getData() instanceof OpenTaskCreatedResult) {
                    throw new OpenApiException(ex.getCode(), ex.getMessage(),
                            toCreateResponse((OpenTaskCreatedResult) ex.getData()));
                }
                throw ex;
            }
        });
    }

    @Override
    public ApiResponse<TaskProgressDto> getTask(String taskId) {
        return invocationPipeline.invoke(OpenApiOperations.GET_TASK,
                ctx -> toProgressDto(openTaskDomainService.get(ctx, taskId)));
    }

    @Override
    public ApiResponse<TaskListPageDto> listTasks(String extTaskId, String status,
                                                  String createdFrom, String createdTo,
                                                  int page, int size) {
        OpenTaskListQuery query = new OpenTaskListQuery();
        query.setExtTaskId(extTaskId);
        query.setStatus(status);
        query.setCreatedFrom(parseUtc(createdFrom));
        query.setCreatedTo(parseUtc(createdTo));
        query.setPage(page);
        query.setSize(size);
        return invocationPipeline.invoke(OpenApiOperations.LIST_TASKS,
                ctx -> toListPageDto(openTaskDomainService.list(ctx, query)));
    }

    private void validateRequest(CreateTaskRequest request) {
        if (request == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "请求体不能为空");
        }
        java.util.Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String msg = violations.iterator().next().getMessage();
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, msg);
        }
    }

    private CreateOpenTaskCommand toCommand(CreateTaskRequest request) {
        CreateOpenTaskCommand command = new CreateOpenTaskCommand();
        command.setExtTaskId(request.getExtTaskId());
        command.setTaskName(request.getTaskName());
        command.setTargets(request.getTargets());
        command.setTargetType(request.getTargetType());
        command.setVulnType(request.getVulnType());
        command.setCallbackUrl(request.getCallbackUrl());
        command.setScanTemplateId(request.getScanTemplateId());
        command.setPriority(request.getPriority());
        command.setOptions(request.getOptions());
        return command;
    }

    private CreateTaskResponse toCreateResponse(OpenTaskCreatedResult result) {
        CreateTaskResponse resp = new CreateTaskResponse();
        resp.setExtTaskId(result.getExtTaskId());
        resp.setTaskId(result.getTaskId());
        resp.setStatus(result.getStatus());
        resp.setCreatedAt(result.getCreatedAt());
        return resp;
    }

    private TaskProgressDto toProgressDto(OpenTaskProgressResult result) {
        TaskProgressDto dto = new TaskProgressDto();
        dto.setExtTaskId(result.getExtTaskId());
        dto.setTaskId(result.getTaskId());
        dto.setStatus(result.getStatus());
        dto.setProgress(result.getProgress());
        dto.setStartedAt(result.getStartedAt());
        dto.setFinishedAt(result.getFinishedAt());
        dto.setErrorMessage(result.getErrorMessage());
        return dto;
    }

    private TaskListPageDto toListPageDto(OpenTaskListResult result) {
        TaskListPageDto dto = new TaskListPageDto();
        dto.setPage(result.getPage());
        dto.setSize(result.getSize());
        dto.setTotal(result.getTotal());
        if (result.getItems() != null) {
            dto.setItems(result.getItems().stream().map(this::toSummaryDto).collect(Collectors.toList()));
        }
        return dto;
    }

    private TaskSummaryDto toSummaryDto(OpenTaskSummaryResult result) {
        TaskSummaryDto dto = new TaskSummaryDto();
        dto.setExtTaskId(result.getExtTaskId());
        dto.setTaskId(result.getTaskId());
        dto.setTaskName(result.getTaskName());
        dto.setStatus(result.getStatus());
        dto.setProgress(result.getProgress());
        dto.setStartedAt(result.getStartedAt());
        dto.setFinishedAt(result.getFinishedAt());
        dto.setErrorMessage(result.getErrorMessage());
        dto.setCreatedAt(result.getCreatedAt());
        return dto;
    }

    private static Date parseUtc(String value) {
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
