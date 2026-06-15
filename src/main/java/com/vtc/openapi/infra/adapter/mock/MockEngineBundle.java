package com.vtc.openapi.infra.adapter.mock;

import com.alibaba.fastjson.JSONObject;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class MockEngineBundle {

    private static final int DEFAULT_SCAN_TEMPLATE_ID = 1001;

    private String bundleId;
    private String description;
    private String extTaskIdPrefix;
    private String taskNameContains;
    private Integer scanTemplateId;
    private List<Integer> reportTemplateIds = new ArrayList<>();
    private List<Integer> vulnTypes = new ArrayList<>();
    private List<JSONObject> instances = new ArrayList<>();

    /**
     * Match score for bundle resolution (higher wins).
     */
    public int matchScore(String extTaskId, String taskName,
                          Integer scanTemplateId, Integer reportTemplateId, Integer vulnType) {
        int score = 0;
        if (this.scanTemplateId != null) {
            int effectiveScan = scanTemplateId == null || scanTemplateId <= 0
                    ? DEFAULT_SCAN_TEMPLATE_ID : scanTemplateId;
            if (!this.scanTemplateId.equals(effectiveScan)) {
                return 0;
            }
            score += 100;
            if (!reportTemplateIds.isEmpty()) {
                if (reportTemplateId == null || reportTemplateId <= 0) {
                    score += 5;
                } else if (reportTemplateIds.contains(reportTemplateId)) {
                    score += 20;
                } else {
                    return 0;
                }
            }
            if (!vulnTypes.isEmpty()) {
                if (vulnType == null) {
                    score += 5;
                } else if (vulnTypes.contains(vulnType)) {
                    score += 20;
                } else {
                    return 0;
                }
            }
        }
        if (StringUtils.hasText(extTaskIdPrefix) && StringUtils.hasText(extTaskId)) {
            if (extTaskId.startsWith(extTaskIdPrefix)) {
                score += 50;
            }
        }
        if (StringUtils.hasText(taskNameContains) && StringUtils.hasText(taskName)) {
            if (taskName.contains(taskNameContains)) {
                score += 30;
            }
        }
        return score;
    }

    public boolean matches(String extTaskId, String taskName) {
        return matchScore(extTaskId, taskName, null, null, null) > 0;
    }

    public String getBundleId() {
        return bundleId;
    }

    public void setBundleId(String bundleId) {
        this.bundleId = bundleId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExtTaskIdPrefix() {
        return extTaskIdPrefix;
    }

    public void setExtTaskIdPrefix(String extTaskIdPrefix) {
        this.extTaskIdPrefix = extTaskIdPrefix;
    }

    public String getTaskNameContains() {
        return taskNameContains;
    }

    public void setTaskNameContains(String taskNameContains) {
        this.taskNameContains = taskNameContains;
    }

    public Integer getScanTemplateId() {
        return scanTemplateId;
    }

    public void setScanTemplateId(Integer scanTemplateId) {
        this.scanTemplateId = scanTemplateId;
    }

    public List<Integer> getReportTemplateIds() {
        return reportTemplateIds;
    }

    public void setReportTemplateIds(List<Integer> reportTemplateIds) {
        this.reportTemplateIds = reportTemplateIds != null ? reportTemplateIds : new ArrayList<>();
    }

    public List<Integer> getVulnTypes() {
        return vulnTypes;
    }

    public void setVulnTypes(List<Integer> vulnTypes) {
        this.vulnTypes = vulnTypes != null ? vulnTypes : new ArrayList<>();
    }

    public List<JSONObject> getInstances() {
        return instances;
    }

    public void setInstances(List<JSONObject> instances) {
        this.instances = instances != null ? instances : new ArrayList<>();
    }
}
