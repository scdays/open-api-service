package com.vtc.openapi.domain.task.model.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ScanEngineCreateCommand {

    private String taskName;

    private List<String> targets;

    private String targetType;

    private Integer vulnType;

    private Integer scanTemplateId;

    private String priority;

    private Map<String, Object> options;
}
