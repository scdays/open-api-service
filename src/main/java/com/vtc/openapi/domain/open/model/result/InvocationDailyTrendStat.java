package com.vtc.openapi.domain.open.model.result;

import java.time.LocalDate;

public class InvocationDailyTrendStat {

    private LocalDate statDate;
    private long totalCount;
    private long successCount;

    public LocalDate getStatDate() {
        return statDate;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(long successCount) {
        this.successCount = successCount;
    }
}
