package com.vtc.openapi.app.convert;

import com.vtc.openapi.domain.task.model.command.CreateOpenTaskCommand;
import com.vtc.openapi.domain.task.model.result.ParsedScanTaskFileResult;
import com.vtc.openapi.domain.task.model.vo.JumpHostEntry;
import com.vtc.openapi.domain.task.model.vo.ScanTargetAuthEntry;
import com.vtc.openapi.domain.task.model.vo.ScanTaskTargets;
import com.vtc.openapi.ui.dto.open.task.CreateScanTaskByFileRequest;
import com.vtc.openapi.ui.dto.open.task.CreateScanTaskByJsonRequest;
import com.vtc.openapi.ui.dto.open.task.JumpHostDto;
import com.vtc.openapi.ui.dto.open.task.ScanTargetAuthEntryDto;
import com.vtc.openapi.ui.dto.open.task.ScanTaskTargetsDto;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OpenTaskAppConvertor {

    public CreateOpenTaskCommand fromJsonRequest(CreateScanTaskByJsonRequest request) {
        CreateOpenTaskCommand command = new CreateOpenTaskCommand();
        command.setExtTaskId(request.getExtTaskId());
        command.setTaskName(request.getTaskName());
        command.setType(request.getType());
        command.setTargets(toDomainTargets(request.getTargets()));
        command.setScanTemplateId(normalizeTemplateId(request.getScanTemplateId()));
        command.setReportTemplateId(normalizeTemplateId(request.getReportTemplateId()));
        command.setCallbackUrl(request.getCallbackUrl());
        command.setPriority(request.getPriority());
        command.setSrcMethod(request.getSrcMethod());
        command.setVulIDs(request.getVulIDs());
        command.setSecResourceHashes(request.getSecResourceHashes());
        command.setAutoVerify(request.getAutoVerify());
        command.setOptions(buildExtensionOptions(command));
        return command;
    }

    public CreateOpenTaskCommand fromFileRequest(CreateScanTaskByFileRequest request,
                                                 ParsedScanTaskFileResult parsed) {
        CreateOpenTaskCommand command = new CreateOpenTaskCommand();
        command.setExtTaskId(request.getExtTaskId());
        command.setType(request.getType());
        command.setTaskName(parsed.getTaskName());
        command.setTargets(parsed.getTargets());
        command.setScanTemplateId(normalizeTemplateId(parsed.getScanTemplateId()));
        command.setReportTemplateId(normalizeTemplateId(parsed.getReportTemplateId()));
        command.setCallbackUrl(parsed.getCallbackUrl());
        command.setPriority(parsed.getPriority());
        command.setFileXml(parsed.getFileXml());
        command.setOptions(buildExtensionOptions(command));
        return command;
    }

    private ScanTaskTargets toDomainTargets(ScanTaskTargetsDto dto) {
        if (dto == null) {
            return null;
        }
        ScanTaskTargets targets = new ScanTaskTargets();
        targets.setHosts(dto.getHosts());
        if (!CollectionUtils.isEmpty(dto.getAuth())) {
            targets.setAuth(dto.getAuth().stream().map(this::toDomainAuth).collect(Collectors.toList()));
        }
        return targets;
    }

    private ScanTargetAuthEntry toDomainAuth(ScanTargetAuthEntryDto dto) {
        ScanTargetAuthEntry entry = new ScanTargetAuthEntry();
        entry.setIp(dto.getIp());
        entry.setProtocol(dto.getProtocol());
        entry.setPort(dto.getPort());
        entry.setUsername(dto.getUsername());
        entry.setPassword(dto.getPassword());
        if (!CollectionUtils.isEmpty(dto.getJumpHosts())) {
            entry.setJumpHosts(dto.getJumpHosts().stream().map(this::toDomainJumpHost).collect(Collectors.toList()));
        }
        return entry;
    }

    private JumpHostEntry toDomainJumpHost(JumpHostDto dto) {
        JumpHostEntry entry = new JumpHostEntry();
        entry.setIp(dto.getIp());
        entry.setProtocol(dto.getProtocol());
        entry.setPort(dto.getPort());
        entry.setUsername(dto.getUsername());
        entry.setPassword(dto.getPassword());
        return entry;
    }

    private Map<String, Object> buildExtensionOptions(CreateOpenTaskCommand command) {
        Map<String, Object> options = new HashMap<>();
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
        if (command.getFileXml() != null) {
            options.put("fileXml", command.getFileXml());
        }
        return options.isEmpty() ? null : options;
    }

    private Integer normalizeTemplateId(Integer templateId) {
        if (templateId == null || templateId <= 0) {
            return null;
        }
        return templateId;
    }
}
