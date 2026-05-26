package com.vtc.openapi.domain.open.model.result;

import java.util.List;

public class PartnerInvocationStatsResult {

    private String partnerId;
    private long todayTotal;
    private long todaySuccess;
    private double todaySuccessRate;
    private List<InvocationErrorCodeStat> topErrorCodes;
    private List<InvocationDailyTrendStat> dailyTrend;

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public long getTodayTotal() {
        return todayTotal;
    }

    public void setTodayTotal(long todayTotal) {
        this.todayTotal = todayTotal;
    }

    public long getTodaySuccess() {
        return todaySuccess;
    }

    public void setTodaySuccess(long todaySuccess) {
        this.todaySuccess = todaySuccess;
    }

    public double getTodaySuccessRate() {
        return todaySuccessRate;
    }

    public void setTodaySuccessRate(double todaySuccessRate) {
        this.todaySuccessRate = todaySuccessRate;
    }

    public List<InvocationErrorCodeStat> getTopErrorCodes() {
        return topErrorCodes;
    }

    public void setTopErrorCodes(List<InvocationErrorCodeStat> topErrorCodes) {
        this.topErrorCodes = topErrorCodes;
    }

    public List<InvocationDailyTrendStat> getDailyTrend() {
        return dailyTrend;
    }

    public void setDailyTrend(List<InvocationDailyTrendStat> dailyTrend) {
        this.dailyTrend = dailyTrend;
    }
}
