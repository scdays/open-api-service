package com.vtc.openapi.infra.adapter;

import com.vtc.openapi.infra.adapter.dto.SvmpTaskCreateRequest;
import com.vtc.openapi.infra.adapter.dto.SvmpTaskCreateResult;
import com.vtc.openapi.infra.adapter.dto.SvmpTaskProgressResult;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.infra.adapter.taskcenter.TaskCenterSubProgressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * vuln-task-center 直连引擎适配（interim）：SOC 创建扫描计划 + 轮询回收。
 */
@Component
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class SvmpEngineAdapterTaskCenterImpl implements SvmpEngineAdapter {

    private static final Logger log = LoggerFactory.getLogger(SvmpEngineAdapterTaskCenterImpl.class);

    private final IOpenTaskRepository openTaskRepository;
    private final TaskCenterSubProgressService progressService;

    public SvmpEngineAdapterTaskCenterImpl(IOpenTaskRepository openTaskRepository,
                                           TaskCenterSubProgressService progressService) {
        this.openTaskRepository = openTaskRepository;
        this.progressService = progressService;
        log.warn("Engine adapter mode: TASK-CENTER (vuln-task-center SOC scan)");
    }

    @Override
    public SvmpTaskCreateResult createTask(SvmpTaskCreateRequest request) {
        SvmpTaskCreateResult result = new SvmpTaskCreateResult();
        Object platformTaskId = request.getOptions() != null ? request.getOptions().get("platformTaskId") : null;
        result.setEngineTaskId(platformTaskId != null ? platformTaskId.toString() : "TC-PENDING");
        return result;
    }

    @Override
    public SvmpTaskProgressResult getTaskProgress(String engineTaskId) {
        SvmpTaskProgressResult progress = new SvmpTaskProgressResult();
        if (!StringUtils.hasText(engineTaskId)) {
            progress.setStatus("FAILED");
            progress.setProgress(0);
            progress.setErrorMessage("engineTaskId empty");
            return progress;
        }
        OpenTaskDO task = openTaskRepository.findByEngineTaskId(engineTaskId);
        if (task == null) {
            task = openTaskRepository.findByTaskId(engineTaskId);
        }
        if (task == null) {
            progress.setStatus("FAILED");
            progress.setProgress(0);
            progress.setErrorMessage("task not found: " + engineTaskId);
            return progress;
        }
        progressService.refreshTask(task);
        task = openTaskRepository.findByTaskId(task.getTaskId());
        progress.setStatus(task.getStatus() != null ? task.getStatus() : "RUNNING");
        progress.setProgress(task.getProgress() != null ? task.getProgress() : 0);
        progress.setErrorMessage(task.getErrorMessage());
        return progress;
    }
}
