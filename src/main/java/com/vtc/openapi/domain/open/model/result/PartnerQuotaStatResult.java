package com.vtc.openapi.domain.open.model.result;

public class PartnerQuotaStatResult {

    private String partnerId;
    private long totalInvocations;
    private long successInvocations;
    private long failedInvocations;
    private double successRate;

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public long getTotalInvocations() {
        return totalInvocations;
    }

    public void setTotalInvocations(long totalInvocations) {
        this.totalInvocations = totalInvocations;
    }

    public long getSuccessInvocations() {
        return successInvocations;
    }

    public void setSuccessInvocations(long successInvocations) {
        this.successInvocations = successInvocations;
    }

    public long getFailedInvocations() {
        return failedInvocations;
    }

    public void setFailedInvocations(long failedInvocations) {
        this.failedInvocations = failedInvocations;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }
}
