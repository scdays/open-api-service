package com.vtc.openapi.domain.open;

/**
 * OpenAPI operationId 常量（与 api_operation 种子数据对齐）。
 */
public final class OpenApiOperations {

    /** @deprecated 契约 F0 起使用 {@link #CREATE_TASK_BY_JSON} / {@link #CREATE_TASK_BY_FILE} */
    @Deprecated
    public static final String CREATE_TASK = "createTask";

    public static final String CREATE_TASK_BY_JSON = "createTaskByJson";
    public static final String CREATE_TASK_BY_FILE = "createTaskByFile";
    /** multipart 文件上传创建任务（与 {@link #CREATE_TASK_BY_FILE} 同能力码 TASK_WRITE） */
    public static final String CREATE_TASK_BY_UPLOAD = "createTaskByUpload";
    public static final String LIST_TASKS = "listTasks";
    public static final String GET_TASK = "getTask";

    public static final String RESOURCE_TYPE_TASK = "TASK";
    public static final String RESOURCE_TYPE_INSTANCE = "INSTANCE";

    // --- OP-OPENAPI-P1 实例 ---
    public static final String SEARCH_INSTANCES = "searchInstances";
    public static final String GET_INSTANCE = "getInstance";
    public static final String VERIFY_INSTANCE = "verifyInstance";
    public static final String REMEDIATE_INSTANCE = "remediateInstance";
    public static final String VERIFY_FIX_INSTANCE = "verifyFixInstance";
    public static final String VERIFY_INSTANCE_BATCH = "verifyInstanceBatch";
    public static final String REMEDIATE_INSTANCE_BATCH = "remediateInstanceBatch";
    public static final String VERIFY_FIX_INSTANCE_BATCH = "verifyFixInstanceBatch";

    // --- OP-OPENAPI-P2 外发 ---
    public static final String GET_EXPORT = "getExport";
    public static final String DOWNLOAD_EXPORT = "downloadExport";
    public static final String LIST_TASK_EXPORTS = "listTaskExports";

    public static final String RESOURCE_TYPE_EXPORT = "EXPORT";

    private OpenApiOperations() {
    }
}
