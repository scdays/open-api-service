package com.vtc.openapi.domain.open.model.query;

import java.util.Date;

/**
 * 内部管理端调用记录查询条件。
 */
public class InvocationAdminQuery {

    private String partnerId;
    private String operationId;
    /** 业务域：TASK / INSTANCE / EXPORT / AUTH */
    private String domain;
    private Integer responseCode;
    private String resourceType;
    private String resourceId;
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

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(Integer responseCode) {
        this.responseCode = responseCode;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
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
