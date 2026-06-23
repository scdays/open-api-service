package com.vtc.openapi.domain.open;

/**
 * OpenAPI operationId 常量（与 api_operation 种子数据对齐）。
 */
public final class OpenApiOperations {

    /** @deprecated 契约 F0 起使用 {@link #CREATE_TASK_BY_JSON} / {@link #CREATE_TASK_BY_FILE} */
    @Deprecated
    public static final String CREATE_TASK = "createTask";

    // --- AUTH ---
    public static final String ISSUE_PARTNER_TOKEN = "issuePartnerToken";

    public static final String CREATE_TASK_BY_JSON = "createTaskByJson";
    public static final String CREATE_TASK_BY_FILE = "createTaskByFile";
    /** multipart 文件上传创建任务（与 {@link #CREATE_TASK_BY_FILE} 同能力码 TASK_WRITE） */
    public static final String CREATE_TASK_BY_UPLOAD = "createTaskByUpload";
    public static final String LIST_TASKS = "listTasks";
    public static final String GET_TASK = "getTask";

    public static final String RESOURCE_TYPE_TASK = "TASK";
    public static final String RESOURCE_TYPE_INSTANCE = "INSTANCE";
    public static final String RESOURCE_TYPE_AUTH = "AUTH";

    // --- OP-OPENAPI-P1 实例 ---
    public static final String SEARCH_INSTANCES = "searchInstances";
    public static final String GET_INSTANCE = "getInstance";
    public static final String VERIFY_INSTANCE = "verifyInstance";
    public static final String REMEDIATE_INSTANCE = "remediateInstance";
    public static final String VERIFY_FIX_INSTANCE = "verifyFixInstance";
    public static final String VERIFY_INSTANCE_BATCH = "verifyInstanceBatch";
    public static final String REMEDIATE_INSTANCE_BATCH = "remediateInstanceBatch";
    public static final String VERIFY_FIX_INSTANCE_BATCH = "verifyFixInstanceBatch";
    public static final String ARCHIVE_INSTANCE = "archiveInstance";
    /** @deprecated 备案兼容别名，外部契约已合并到 {@link #REMEDIATE_INSTANCE}。 */
    @Deprecated
    public static final String ARCHIVE_INSTANCE_LEGACY = "archiveInstanceLegacy";

    // --- OP-OPENAPI-P2 外发 ---
    public static final String GET_EXPORT = "getExport";
    public static final String DOWNLOAD_EXPORT = "downloadExport";
    public static final String LIST_TASK_EXPORTS = "listTaskExports";

    public static final String RESOURCE_TYPE_EXPORT = "EXPORT";

    // --- OP-OPENAPI-P2 产物 ---
    public static final String GET_ARTIFACT = "getArtifact";
    public static final String DOWNLOAD_ARTIFACT = "downloadArtifact";
    public static final String LIST_TASK_ARTIFACTS = "listTaskArtifacts";
    public static final String LIST_EXPORT_ARTIFACTS = "listExportArtifacts";

    public static final String RESOURCE_TYPE_ARTIFACT = "ARTIFACT";

    // --- WEBHOOK ---
    public static final String RECEIVE_PLATFORM_WEBHOOK = "receivePlatformWebhook";

    /** 运营案件统一句柄（api_invocation.resource_type） */
    public static final String RESOURCE_TYPE_CASE = "CASE";

    public static final String PRIMARY_RESOURCE_TASK = "TASK";
    public static final String PRIMARY_RESOURCE_INSTANCE = "INSTANCE";
    public static final String PRIMARY_RESOURCE_VERIFY_FIX_JOB = "VERIFY_FIX_JOB";

    private OpenApiOperations() {
    }
}
