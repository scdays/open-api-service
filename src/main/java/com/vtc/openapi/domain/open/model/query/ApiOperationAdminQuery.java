package com.vtc.openapi.domain.open.model.query;

/**
 * 内部管理端 API 目录查询条件。
 */
public class ApiOperationAdminQuery {

    private String requiredCapability;
    private String status;
    private String openapiTag;
    private String domain;
    private String operationId;
    private int page;
    private int size;

    public String getRequiredCapability() {
        return requiredCapability;
    }

    public void setRequiredCapability(String requiredCapability) {
        this.requiredCapability = requiredCapability;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOpenapiTag() {
        return openapiTag;
    }

    public void setOpenapiTag(String openapiTag) {
        this.openapiTag = openapiTag;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
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
