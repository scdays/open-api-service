package com.vtc.openapi.infra.dao.data;

import java.util.Date;

public class InvocationDailyStatRow {

    private Date statDay;
    private Long totalCount;
    private Long successCount;

    public Date getStatDay() {
        return statDay;
    }

    public void setStatDay(Date statDay) {
        this.statDay = statDay;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Long successCount) {
        this.successCount = successCount;
    }
}
