package com.vtc.openapi.domain.operationcase.model;

/**
 * 运营案件类型（与方案 §2.1 对齐）。
 */
public final class OperationCaseTypes {

    public static final String TASK_SCAN = "TASK_SCAN";
    public static final String INSTANCE_VERIFY = "INSTANCE_VERIFY";
    public static final String INSTANCE_REMEDIATE = "INSTANCE_REMEDIATE";
    public static final String VERIFY_FIX = "VERIFY_FIX";
    public static final String INSTANCE_BATCH = "INSTANCE_BATCH";

    private OperationCaseTypes() {
    }
}
