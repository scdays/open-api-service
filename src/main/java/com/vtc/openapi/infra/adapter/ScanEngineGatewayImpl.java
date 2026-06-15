package com.vtc.openapi.infra.adapter;

import com.vtc.openapi.domain.task.gateway.IScanEngineGateway;
import com.vtc.openapi.domain.task.model.support.TaskTypeSupport;
import com.vtc.openapi.domain.task.model.vo.ScanEngineCreateCommand;
import com.vtc.openapi.domain.task.model.vo.ScanEngineCreateResult;
import com.vtc.openapi.domain.task.model.vo.ScanEngineProgressResult;
import com.vtc.openapi.infra.adapter.dto.SvmpTaskCreateRequest;
import com.vtc.openapi.infra.adapter.dto.SvmpTaskCreateResult;
import com.vtc.openapi.infra.adapter.dto.SvmpTaskProgressResult;
import org.springframework.stereotype.Component;

@Component
public class ScanEngineGatewayImpl implements IScanEngineGateway {

    private final SvmpEngineAdapter svmpEngineAdapter;

    public ScanEngineGatewayImpl(SvmpEngineAdapter svmpEngineAdapter) {
        this.svmpEngineAdapter = svmpEngineAdapter;
    }

    @Override
    public ScanEngineCreateResult createTask(ScanEngineCreateCommand command) {
        SvmpTaskCreateRequest request = new SvmpTaskCreateRequest();
        request.setTaskName(command.getTaskName());
        request.setTargets(command.getTargets());
        request.setTargetType(command.getTargetType());
        request.setVulnType(command.getType());
        request.setScanTemplateId(command.getScanTemplateId());
        request.setPriority(command.getPriority());
        request.setOptions(command.getOptions());
        SvmpTaskCreateResult result = svmpEngineAdapter.createTask(request);
        ScanEngineCreateResult domain = new ScanEngineCreateResult();
        domain.setEngineTaskId(result.getEngineTaskId());
        return domain;
    }

    @Override
    public ScanEngineProgressResult getTaskProgress(String engineTaskId) {
        SvmpTaskProgressResult progress = svmpEngineAdapter.getTaskProgress(engineTaskId);
        ScanEngineProgressResult domain = new ScanEngineProgressResult();
        domain.setStatus(TaskTypeSupport.normalizeProgressStatus(progress.getStatus()));
        domain.setProgress(progress.getProgress());
        domain.setErrorMessage(progress.getErrorMessage());
        return domain;
    }
}
