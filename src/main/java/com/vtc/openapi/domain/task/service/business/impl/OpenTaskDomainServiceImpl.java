package com.vtc.openapi.domain.task.service.business.impl;

import com.alibaba.fastjson.JSON;
import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.domain.service.DomainServiceImpl;
import com.vtc.openapi.domain.instance.service.business.IInstanceIngestDomainService;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.open.OpenApiOperations;
import com.vtc.openapi.domain.open.model.InvocationContext;
import com.vtc.openapi.domain.partner.context.PartnerContext;
import com.vtc.openapi.domain.task.gateway.IScanEngineGateway;
import com.vtc.openapi.domain.task.model.command.CreateOpenTaskCommand;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.entity.PartnerTaskMapDO;
import com.vtc.openapi.domain.task.model.query.OpenTaskListQuery;
import com.vtc.openapi.domain.task.model.result.OpenTaskCreatedResult;
import com.vtc.openapi.domain.task.model.result.OpenTaskListResult;
import com.vtc.openapi.domain.task.model.result.OpenTaskProgressResult;
import com.vtc.openapi.domain.task.model.result.OpenTaskSummaryResult;
import com.vtc.openapi.domain.task.model.support.TaskTypeSupport;
import com.vtc.openapi.domain.task.model.vo.ScanEngineCreateCommand;
import com.vtc.openapi.domain.task.model.vo.ScanEngineCreateResult;
import com.vtc.openapi.domain.task.model.vo.ScanEngineProgressResult;
import com.vtc.openapi.domain.task.model.vo.ScanTaskTargets;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.domain.task.service.MockTaskCompletionCoordinator;
import com.vtc.openapi.domain.task.service.business.IOpenTaskDomainService;
import com.vtc.openapi.domain.webhook.service.business.IWebhookPublishService;
import com.vtc.openapi.infra.adapter.taskcenter.TaskCenterPostAcceptDispatcher;
import com.vtc.openapi.infra.adapter.taskcenter.TaskCenterScannerPlanner;
import com.vtc.openapi.infra.adapter.taskcenter.TaskCenterSubSupport;
import com.vtc.openapi.infra.config.OpenApiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OpenTaskDomainServiceImpl
        extends DomainServiceImpl<IOpenTaskRepository, OpenTaskDO>
        implements IOpenTaskDomainService {

    private static final SimpleDateFormat ISO_UTC;

    static {
        ISO_UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        ISO_UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final IScanEngineGateway scanEngineGateway;
    private final IInstanceIngestDomainService instanceIngestDomainService;
    private final IWebhookPublishService webhookPublishService;
    private final MockTaskCompletionCoordinator taskCompletionCoordinator;
    private final TaskCenterPostAcceptDispatcher taskCenterPostAcceptDispatcher;
    private final OpenApiProperties openApiProperties;

    public OpenTaskDomainServiceImpl(IScanEngineGateway scanEngineGateway,
                                     IInstanceIngestDomainService instanceIngestDomainService,
                                     IWebhookPublishService webhookPublishService,
                                     @Autowired(required = false) MockTaskCompletionCoordinator taskCompletionCoordinator,
                                     @Autowired(required = false) TaskCenterPostAcceptDispatcher taskCenterPostAcceptDispatcher,
                                     OpenApiProperties openApiProperties) {
        this.scanEngineGateway = scanEngineGateway;
        this.instanceIngestDomainService = instanceIngestDomainService;
        this.webhookPublishService = webhookPublishService;
        this.taskCompletionCoordinator = taskCompletionCoordinator;
        this.taskCenterPostAcceptDispatcher = taskCenterPostAcceptDispatcher;
        this.openApiProperties = openApiProperties;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpenTaskCreatedResult create(InvocationContext ctx, CreateOpenTaskCommand command) {
        validateCreateCommand(command);
        String partnerId = PartnerContext.requirePartnerId();

        PartnerTaskMapDO existingMap = databaseRepository.findTaskMap(partnerId, command.getExtTaskId());
        if (existingMap != null) {
            OpenTaskDO existingTask = databaseRepository.findByTaskId(existingMap.getPlatformTaskId());
            ctx.setResourceType(OpenApiOperations.RESOURCE_TYPE_TASK);
            ctx.setResourceId(existingTask.getTaskId());
            throw new OpenApiException(OpenApiConstants.CODE_IDEMPOTENT_CONFLICT, "extTaskId 已存在",
                    toCreatedResult(existingTask, command.getExtTaskId()));
        }

        String platformTaskId = "TASK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        ScanEngineCreateResult engineResult = scanEngineGateway.createTask(
                toEngineCommand(command, isTaskCenterMode() ? platformTaskId : null));
        Date now = new Date();

        OpenTaskDO task = new OpenTaskDO();
        task.setTaskId(platformTaskId);
        task.setPartnerId(partnerId);
        task.setExtTaskId(command.getExtTaskId());
        task.setEngineTaskId(isTaskCenterMode() ? platformTaskId : engineResult.getEngineTaskId());
        task.setTaskName(command.getTaskName());
        task.setTargetType(TaskTypeSupport.resolveTargetType(command.getType()));
        task.setVulnType(command.getType());
        task.setTargetsJson(JSON.toJSONString(command.getTargets()));
        task.setStatus(OpenApiConstants.TASK_ACCEPT_ACCEPTED);
        task.setProgress(0);
        task.setScanTemplateId(command.getScanTemplateId());
        task.setReportTemplateId(command.getReportTemplateId());
        task.setCallbackUrl(command.getCallbackUrl());
        if (isTaskCenterMode()) {
            task.setTaskPhase(TaskCenterSubSupport.PHASE_SURVEY);
            task.setAutoVerify(resolveAutoVerify(command));
            task.setCrossScan(TaskCenterScannerPlanner.isCrossScan(command.getScanTemplateId()));
            task.setVerifyMergeStrategy(
                    TaskCenterScannerPlanner.resolveVerifyMergeStrategy(command.getScanTemplateId()));
        }
        if (command.getOptions() != null) {
            task.setOptionsJson(JSON.toJSONString(command.getOptions()));
        }
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        PartnerTaskMapDO map = new PartnerTaskMapDO();
        map.setPartnerId(partnerId);
        map.setExtTaskId(command.getExtTaskId());
        map.setPlatformTaskId(platformTaskId);
        map.setCreatedAt(now);

        try {
            databaseRepository.save(task);
            databaseRepository.saveTaskMap(map);
        } catch (DuplicateKeyException ex) {
            PartnerTaskMapDO raced = databaseRepository.findTaskMap(partnerId, command.getExtTaskId());
            OpenTaskDO racedTask = databaseRepository.findByTaskId(raced.getPlatformTaskId());
            ctx.setResourceType(OpenApiOperations.RESOURCE_TYPE_TASK);
            ctx.setResourceId(racedTask.getTaskId());
            throw new OpenApiException(OpenApiConstants.CODE_IDEMPOTENT_CONFLICT, "extTaskId 已存在",
                    toCreatedResult(racedTask, command.getExtTaskId()));
        }

        ctx.setResourceType(OpenApiOperations.RESOURCE_TYPE_TASK);
        ctx.setResourceId(platformTaskId);
        if (taskCenterPostAcceptDispatcher != null) {
            taskCenterPostAcceptDispatcher.scheduleSurveyDispatch(platformTaskId);
        }
        return toCreatedResult(task, command.getExtTaskId());
    }

    @Override
    public OpenTaskProgressResult get(InvocationContext ctx, String taskId) {
        if (!StringUtils.hasText(taskId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "taskId 不能为空");
        }
        String partnerId = PartnerContext.requirePartnerId();
        OpenTaskDO task = requireOwnedTask(taskId, partnerId);
        mergeEngineProgress(task);
        ctx.setResourceType(OpenApiOperations.RESOURCE_TYPE_TASK);
        ctx.setResourceId(taskId);
        return toProgressResult(task);
    }

    @Override
    public OpenTaskListResult list(InvocationContext ctx, OpenTaskListQuery query) {
        String partnerId = PartnerContext.requirePartnerId();
        if (query.getPage() < 1) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "page 必须从 1 开始");
        }
        if (query.getSize() < 1 || query.getSize() > 1000) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "size 必须在 1-1000 之间");
        }
        PageInfo<OpenTaskDO> pageResult = databaseRepository.pageByPartner(partnerId, query);
        OpenTaskListResult data = new OpenTaskListResult();
        data.setPage(query.getPage());
        data.setSize(query.getSize());
        data.setTotal(pageResult.getTotal());
        if (!CollectionUtils.isEmpty(pageResult.getRecords())) {
            data.setItems(pageResult.getRecords().stream()
                    .map(this::toSummaryResult)
                    .collect(Collectors.toList()));
        }
        return data;
    }

    private void validateCreateCommand(CreateOpenTaskCommand command) {
        if (command == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "请求体不能为空");
        }
        if (!StringUtils.hasText(command.getExtTaskId())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "extTaskId 不能为空");
        }
        if (!StringUtils.hasText(command.getTaskName())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "taskName 不能为空");
        }
        TaskTypeSupport.requireValidType(command.getType());
        ScanTaskTargets targets = command.getTargets();
        if (targets == null || !StringUtils.hasText(targets.getHosts())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "targets.hosts 不能为空");
        }
        TaskTypeSupport.splitHosts(targets.getHosts());
    }

    private OpenTaskDO requireOwnedTask(String taskId, String partnerId) {
        OpenTaskDO task = databaseRepository.findByTaskId(taskId);
        if (task == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "任务不存在");
        }
        if (!Objects.equals(partnerId, task.getPartnerId())) {
            throw new OpenApiException(OpenApiConstants.CODE_CROSS_PARTNER, "无权访问该任务");
        }
        return task;
    }

    private void mergeEngineProgress(OpenTaskDO task) {
        if (!StringUtils.hasText(task.getEngineTaskId())) {
            return;
        }
        boolean wasFinished = "FINISHED".equals(task.getStatus());
        boolean wasFailed = "FAILED".equals(task.getStatus());
        ScanEngineProgressResult progress = scanEngineGateway.getTaskProgress(task.getEngineTaskId());
        if ((wasFinished || wasFailed) && "RUNNING".equals(progress.getStatus())) {
            // mock-manual: operator import already terminal; ignore stale in-memory engine RUNNING.
            return;
        }
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
        databaseRepository.updateById(task);
        if ("FINISHED".equals(progress.getStatus()) && !wasFinished) {
            instanceIngestDomainService.tryIngestOnTaskFinished(task);
            if (taskCompletionCoordinator != null) {
                taskCompletionCoordinator.scheduleNotify(task.getTaskId());
            }
        }
        if ("FAILED".equals(progress.getStatus()) && !wasFailed) {
            webhookPublishService.publishTaskFailed(task);
        }
    }

    private ScanEngineCreateCommand toEngineCommand(CreateOpenTaskCommand command, String platformTaskId) {
        ScanEngineCreateCommand engineReq = new ScanEngineCreateCommand();
        engineReq.setTaskName(command.getTaskName());
        engineReq.setType(command.getType());
        engineReq.setTargets(TaskTypeSupport.splitHosts(command.getTargets().getHosts()));
        engineReq.setTargetType(TaskTypeSupport.resolveTargetType(command.getType()));
        engineReq.setScanTemplateId(command.getScanTemplateId());
        engineReq.setPriority(command.getPriority());
        Map<String, Object> options = command.getOptions() != null
                ? new HashMap<>(command.getOptions()) : new HashMap<>();
        options.put("extTaskId", command.getExtTaskId());
        if (StringUtils.hasText(platformTaskId)) {
            options.put("platformTaskId", platformTaskId);
        }
        if (command.getReportTemplateId() != null) {
            options.put("reportTemplateId", command.getReportTemplateId());
        }
        if (command.getSrcMethod() != null) {
            options.put("srcMethod", command.getSrcMethod());
        }
        if (!CollectionUtils.isEmpty(command.getVulIDs())) {
            options.put("vulIDs", command.getVulIDs());
        }
        if (!CollectionUtils.isEmpty(command.getSecResourceHashes())) {
            options.put("secResourceHashes", command.getSecResourceHashes());
        }
        if (command.getTargets() != null && !CollectionUtils.isEmpty(command.getTargets().getAuth())) {
            options.put("auth", command.getTargets().getAuth());
        }
        if (StringUtils.hasText(command.getFileXml())) {
            options.put("fileXml", command.getFileXml());
        }
        engineReq.setOptions(options.isEmpty() ? null : options);
        return engineReq;
    }

    private OpenTaskCreatedResult toCreatedResult(OpenTaskDO task, String extTaskId) {
        OpenTaskCreatedResult resp = new OpenTaskCreatedResult();
        resp.setExtTaskId(extTaskId);
        resp.setTaskId(task.getTaskId());
        resp.setStatus(OpenApiConstants.TASK_ACCEPT_ACCEPTED);
        resp.setCreatedAt(formatUtc(task.getCreatedAt()));
        return resp;
    }

    private OpenTaskProgressResult toProgressResult(OpenTaskDO task) {
        OpenTaskProgressResult dto = new OpenTaskProgressResult();
        dto.setExtTaskId(task.getExtTaskId());
        dto.setTaskId(task.getTaskId());
        dto.setStatus(task.getStatus());
        dto.setProgress(task.getProgress());
        dto.setStartedAt(formatUtc(task.getStartedAt()));
        dto.setFinishedAt(formatUtc(task.getFinishedAt()));
        dto.setErrorMessage(task.getErrorMessage());
        return dto;
    }

    private OpenTaskSummaryResult toSummaryResult(OpenTaskDO task) {
        OpenTaskSummaryResult dto = new OpenTaskSummaryResult();
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

    private boolean isTaskCenterMode() {
        return "task-center".equalsIgnoreCase(openApiProperties.getEngine().getAdapterMode());
    }

    private static boolean resolveAutoVerify(CreateOpenTaskCommand command) {
        if (command.getAutoVerify() != null) {
            return command.getAutoVerify();
        }
        return true;
    }
}
