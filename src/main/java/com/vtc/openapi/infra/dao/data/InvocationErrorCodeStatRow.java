package com.vtc.openapi.infra.dao.data;

public class InvocationErrorCodeStatRow {

    private Integer responseCode;
    private Long count;

    public Integer getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(Integer responseCode) {
        this.responseCode = responseCode;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
