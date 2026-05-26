package com.vtc.openapi.ui.dto.open.task;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public class CreateTaskRequest {

    @NotBlank(message = "extTaskId 不能为空")
    @Size(max = 128)
    private String extTaskId;

    @NotBlank(message = "taskName 不能为空")
    @Size(max = 256)
    private String taskName;

    @NotEmpty(message = "targets 不能为空")
    private List<String> targets;

    @NotBlank(message = "targetType 不能为空")
    private String targetType;

    @NotNull(message = "vulnType 不能为空")
    private Integer vulnType;

    private String callbackUrl;
    private Integer scanTemplateId;
    private String exportTemplateId;
    private String priority;
    private String scheduleTime;
    private Map<String, Object> options;

    public String getExtTaskId() {
        return extTaskId;
    }

    public void setExtTaskId(String extTaskId) {
        this.extTaskId = extTaskId;
    }

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

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public Integer getScanTemplateId() {
        return scanTemplateId;
    }

    public void setScanTemplateId(Integer scanTemplateId) {
        this.scanTemplateId = scanTemplateId;
    }

    public String getExportTemplateId() {
        return exportTemplateId;
    }

    public void setExportTemplateId(String exportTemplateId) {
        this.exportTemplateId = exportTemplateId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getScheduleTime() {
        return scheduleTime;
    }

    public void setScheduleTime(String scheduleTime) {
        this.scheduleTime = scheduleTime;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }
}
