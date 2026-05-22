package com.vtc.openapi.adapter.dto;

public class SvmpTaskCreateResult {

    private String engineTaskId;

    public SvmpTaskCreateResult() {
    }

    public SvmpTaskCreateResult(String engineTaskId) {
        this.engineTaskId = engineTaskId;
    }

    public String getEngineTaskId() {
        return engineTaskId;
    }

    public void setEngineTaskId(String engineTaskId) {
        this.engineTaskId = engineTaskId;
    }
}
