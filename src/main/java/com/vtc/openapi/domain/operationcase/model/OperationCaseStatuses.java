package com.vtc.openapi.domain.operationcase.model;

/**
 * 运营案件状态机。
 */
public final class OperationCaseStatuses {

    public static final String ACCEPTED = "ACCEPTED";
    public static final String RUNNING = "RUNNING";
    public static final String FINISHED = "FINISHED";
    public static final String FAILED = "FAILED";
    public static final String PARTIAL_FAILED = "PARTIAL_FAILED";
    public static final String CANCELLED = "CANCELLED";

    private OperationCaseStatuses() {
    }
}
