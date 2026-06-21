package com.vtc.openapi.app.service.impl;

import com.vtc.openapi.app.convert.OpenTaskAppConvertor;
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
import com.vtc.openapi.domain.task.model.result.ParsedScanTaskFileResult;
import com.vtc.openapi.domain.task.model.support.TaskTypeSupport;
import com.vtc.openapi.domain.task.service.business.IOpenTaskDomainService;
import com.vtc.openapi.infra.adapter.task.ScanTaskXmlParser;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.open.task.CreateScanTaskByFileRequest;
import com.vtc.openapi.ui.dto.open.task.CreateScanTaskByJsonRequest;
import com.vtc.openapi.ui.dto.open.task.CreateTaskResponse;
import com.vtc.openapi.ui.dto.open.task.TaskListPageDto;
import com.vtc.openapi.ui.dto.open.task.TaskProgressDto;
import com.vtc.openapi.ui.dto.open.task.TaskSummaryDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * 任务应用服务：DTO 转换 + InvocationPipeline 编排。
 */
@Service
public class OpenTaskAppServiceImpl implements IOpenTaskAppService {

    private static final DateTimeFormatter ISO_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private final InvocationPipeline invocationPipeline;
    private final IOpenTaskDomainService openTaskDomainService;
    private final OpenTaskAppConvertor openTaskAppConvertor;
    private final ScanTaskXmlParser scanTaskXmlParser;
    private final Validator validator;

    public OpenTaskAppServiceImpl(InvocationPipeline invocationPipeline,
                                  IOpenTaskDomainService openTaskDomainService,
                                  OpenTaskAppConvertor openTaskAppConvertor,
                                  ScanTaskXmlParser scanTaskXmlParser,
                                  Validator validator) {
        this.invocationPipeline = invocationPipeline;
        this.openTaskDomainService = openTaskDomainService;
        this.openTaskAppConvertor = openTaskAppConvertor;
        this.scanTaskXmlParser = scanTaskXmlParser;
        this.validator = validator;
    }

    @Override
    public ApiResponse<CreateTaskResponse> createTaskByJson(CreateScanTaskByJsonRequest request) {
        validateRequest(request);
        TaskTypeSupport.requireValidType(request.getType());
        CreateOpenTaskCommand command = openTaskAppConvertor.fromJsonRequest(request);
        return invokeCreate(OpenApiOperations.CREATE_TASK_BY_JSON, command);
    }

    /**
     * @deprecated 改用 {@link #createTaskByUpload(MultipartFile, String, Integer)}。
     */
    @Deprecated
    @Override
    public ApiResponse<CreateTaskResponse> createTaskByFile(CreateScanTaskByFileRequest request) {
        validateRequest(request);
        TaskTypeSupport.requireValidType(request.getType());
        ParsedScanTaskFileResult parsed = scanTaskXmlParser.parse(request.getFile());
        CreateOpenTaskCommand command = openTaskAppConvertor.fromFileRequest(request, parsed);
        return invokeCreate(OpenApiOperations.CREATE_TASK_BY_FILE, command);
    }

    @Override
    public ApiResponse<CreateTaskResponse> createTaskByUpload(MultipartFile file, String extTaskId, Integer type) {
        if (file == null || file.isEmpty()) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "file 不能为空");
        }
        if (!StringUtils.hasText(extTaskId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "extTaskId 不能为空");
        }
        if (type == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "type 不能为空");
        }
        TaskTypeSupport.requireValidType(type);

        String fileXml = readFileContent(file);
        ParsedScanTaskFileResult parsed = scanTaskXmlParser.parse(fileXml);

        CreateScanTaskByFileRequest legacyRequest = new CreateScanTaskByFileRequest();
        legacyRequest.setExtTaskId(extTaskId);
        legacyRequest.setType(type);
        legacyRequest.setFile(fileXml);

        CreateOpenTaskCommand command = openTaskAppConvertor.fromFileRequest(legacyRequest, parsed);
        return invokeCreate(OpenApiOperations.CREATE_TASK_BY_UPLOAD, command);
    }

    /** 读取上传文件为 UTF-8 字符串 */
    private String readFileContent(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName != null && !originalName.toLowerCase().endsWith(".xml")) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "仅支持 .xml 文件");
        }
        try {
            byte[] bytes = file.getBytes();
            String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            if (!StringUtils.hasText(content)) {
                throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "文件内容为空");
            }
            return content;
        } catch (OpenApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "读取上传文件失败: " + ex.getMessage());
        }
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

    private ApiResponse<CreateTaskResponse> invokeCreate(String operationId, CreateOpenTaskCommand command) {
        return invocationPipeline.invoke(operationId, ctx -> {
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

    private <T> void validateRequest(T request) {
        if (request == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "请求体不能为空");
        }
        java.util.Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String msg = violations.iterator().next().getMessage();
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, msg);
        }
    }

    private CreateTaskResponse toCreateResponse(OpenTaskCreatedResult result) {
        CreateTaskResponse resp = new CreateTaskResponse();
        resp.setExtTaskId(result.getExtTaskId());
        resp.setTaskId(result.getTaskId());
        resp.setStatus(result.getStatus());
        resp.setCreatedAt(result.getCreatedAt());
        resp.setMessage(result.getMessage());
        return resp;
    }

    private TaskProgressDto toProgressDto(OpenTaskProgressResult result) {
        TaskProgressDto dto = new TaskProgressDto();
        dto.setExtTaskId(result.getExtTaskId());
        dto.setTaskId(result.getTaskId());
        dto.setStatus(TaskTypeSupport.normalizeProgressStatus(result.getStatus()));
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
        dto.setStatus(TaskTypeSupport.normalizeProgressStatus(result.getStatus()));
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
            return Date.from(ISO_UTC.parse(value, Instant::from));
        } catch (DateTimeParseException e) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "日期格式无效，需 ISO 8601 UTC");
        }
    }
}
