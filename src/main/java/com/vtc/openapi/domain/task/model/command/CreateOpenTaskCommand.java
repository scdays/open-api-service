package com.vtc.openapi.domain.task.model.command;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CreateOpenTaskCommand {

    private String extTaskId;

    private String taskName;

    private List<String> targets;

    private String targetType;

    private Integer vulnType;

    private String callbackUrl;

    private Integer scanTemplateId;

    private String priority;

    private Map<String, Object> options;
}
