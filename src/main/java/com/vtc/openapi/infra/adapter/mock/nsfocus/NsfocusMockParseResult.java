package com.vtc.openapi.infra.adapter.mock.nsfocus;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsed NSFocus Aurora XML for mock bundle generation.
 */
public class NsfocusMockParseResult {

    private String taskId;

    private String taskName;

    private String taskType;

    private String scanKind;

    private String profile;

    private String sourceXml;

    private String transferTime;

    private String vendor;

    private List<JSONObject> instances = new ArrayList<>();

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getScanKind() {
        return scanKind;
    }

    public void setScanKind(String scanKind) {
        this.scanKind = scanKind;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getSourceXml() {
        return sourceXml;
    }

    public void setSourceXml(String sourceXml) {
        this.sourceXml = sourceXml;
    }

    public String getTransferTime() {
        return transferTime;
    }

    public void setTransferTime(String transferTime) {
        this.transferTime = transferTime;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public List<JSONObject> getInstances() {
        return instances;
    }

    public void setInstances(List<JSONObject> instances) {
        this.instances = instances != null ? instances : new ArrayList<>();
    }
}
