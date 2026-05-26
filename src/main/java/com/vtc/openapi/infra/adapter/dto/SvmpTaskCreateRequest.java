package com.vtc.openapi.infra.adapter.dto;

import java.util.List;
import java.util.Map;

/**
 * 下发 SVMP 引擎的请求体（不含 extTaskId）。
 */
public class SvmpTaskCreateRequest {

    private String taskName;
    private List<String> targets;
    private String targetType;
    private Integer vulnType;
    private Integer scanTemplateId;
    private String priority;
    private Map<String, Object> options;

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public List<String> getTargets() {
        return targets;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Integer getVulnType() {
        return vulnType;
    }

    public void setVulnType(Integer vulnType) {
        this.vulnType = vulnType;
    }

    public Integer getScanTemplateId() {
        return scanTemplateId;
    }

    public void setScanTemplateId(Integer scanTemplateId) {
        this.scanTemplateId = scanTemplateId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }
}
