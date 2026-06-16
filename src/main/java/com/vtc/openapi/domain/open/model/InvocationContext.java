package com.vtc.openapi.domain.open.model;

/**
 * 单次 Partner HTTP 调用的治理上下文（线程内传递）。
 */
public class InvocationContext {

    private final String partnerId;
    private final String requestId;
    private final String operationId;
    private final String httpMethod;
    private final String requestPath;
    private final String clientIp;
    private final long startedAtMillis;
    private String invocationId;
    private String resourceType;
    private String resourceId;
    private String requestBodyJson;

    public InvocationContext(String partnerId, String requestId, String operationId,
                             String httpMethod, String requestPath, String clientIp) {
        this.partnerId = partnerId;
        this.requestId = requestId;
        this.operationId = operationId;
        this.httpMethod = httpMethod;
        this.requestPath = requestPath;
        this.clientIp = clientIp;
        this.startedAtMillis = System.currentTimeMillis();
    }

    public String getPartnerId() {
        return partnerId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public String getClientIp() {
        return clientIp;
    }

    public long getStartedAtMillis() {
        return startedAtMillis;
    }

    public String getInvocationId() {
        return invocationId;
    }

    public void setInvocationId(String invocationId) {
        this.invocationId = invocationId;
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

    public String getRequestBodyJson() {
        return requestBodyJson;
    }

    public void setRequestBodyJson(String requestBodyJson) {
        this.requestBodyJson = requestBodyJson;
    }
}
