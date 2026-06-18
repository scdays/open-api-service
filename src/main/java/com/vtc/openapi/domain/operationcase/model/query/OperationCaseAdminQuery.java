package com.vtc.openapi.domain.operationcase.model.query;

import java.util.Date;

public class OperationCaseAdminQuery {

    private String partnerId;
    private String caseType;
    private String status;
    private String primaryResourceId;
    private String caseId;
    private Date startedFrom;
    private Date startedTo;
    private int page;
    private int size;

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPrimaryResourceId() {
        return primaryResourceId;
    }

    public void setPrimaryResourceId(String primaryResourceId) {
        this.primaryResourceId = primaryResourceId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public Date getStartedFrom() {
        return startedFrom;
    }

    public void setStartedFrom(Date startedFrom) {
        this.startedFrom = startedFrom;
    }

    public Date getStartedTo() {
        return startedTo;
    }

    public void setStartedTo(Date startedTo) {
        this.startedTo = startedTo;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
