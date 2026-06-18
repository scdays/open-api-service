package com.vtc.openapi.domain.operationcase.model.support;

import com.vtc.openapi.domain.open.OpenApiOperations;
import com.vtc.openapi.domain.operationcase.model.OperationCaseTypes;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * operationId → case_type 映射与标题生成。
 */
public final class OperationCaseSupport {

    private static final Map<String, String> OPERATION_TO_CASE_TYPE = new HashMap<>();

    static {
        OPERATION_TO_CASE_TYPE.put(OpenApiOperations.CREATE_TASK, OperationCaseTypes.TASK_SCAN);
        OPERATION_TO_CASE_TYPE.put(OpenApiOperations.CREATE_TASK_BY_JSON, OperationCaseTypes.TASK_SCAN);
        OPERATION_TO_CASE_TYPE.put(OpenApiOperations.CREATE_TASK_BY_FILE, OperationCaseTypes.TASK_SCAN);
        OPERATION_TO_CASE_TYPE.put(OpenApiOperations.CREATE_TASK_BY_UPLOAD, OperationCaseTypes.TASK_SCAN);
        OPERATION_TO_CASE_TYPE.put(OpenApiOperations.VERIFY_INSTANCE, OperationCaseTypes.INSTANCE_VERIFY);
        OPERATION_TO_CASE_TYPE.put(OpenApiOperations.REMEDIATE_INSTANCE, OperationCaseTypes.INSTANCE_REMEDIATE);
        OPERATION_TO_CASE_TYPE.put(OpenApiOperations.VERIFY_FIX_INSTANCE, OperationCaseTypes.VERIFY_FIX);
        OPERATION_TO_CASE_TYPE.put(OpenApiOperations.VERIFY_INSTANCE_BATCH, OperationCaseTypes.INSTANCE_BATCH);
        OPERATION_TO_CASE_TYPE.put(OpenApiOperations.REMEDIATE_INSTANCE_BATCH, OperationCaseTypes.INSTANCE_BATCH);
        OPERATION_TO_CASE_TYPE.put(OpenApiOperations.VERIFY_FIX_INSTANCE_BATCH, OperationCaseTypes.INSTANCE_BATCH);
    }

    private OperationCaseSupport() {
    }

    public static boolean isCaseOperation(String operationId) {
        return StringUtils.hasText(operationId) && OPERATION_TO_CASE_TYPE.containsKey(operationId);
    }

    public static String resolveCaseType(String operationId) {
        return OPERATION_TO_CASE_TYPE.get(operationId);
    }

    public static java.util.List<String> caseOperationIds() {
        return new java.util.ArrayList<>(OPERATION_TO_CASE_TYPE.keySet());
    }

    public static String buildTitle(String caseType, String operationId) {
        if (OperationCaseTypes.TASK_SCAN.equals(caseType)) {
            return "创建扫描任务";
        }
        if (OperationCaseTypes.INSTANCE_VERIFY.equals(caseType)) {
            return "漏洞实例验证";
        }
        if (OperationCaseTypes.INSTANCE_REMEDIATE.equals(caseType)) {
            return "漏洞实例处置";
        }
        if (OperationCaseTypes.VERIFY_FIX.equals(caseType)) {
            return "修复核验";
        }
        if (OperationCaseTypes.INSTANCE_BATCH.equals(caseType)) {
            return "批量实例操作";
        }
        return StringUtils.hasText(operationId) ? operationId : "运营案件";
    }

    public static String truncate(String text, int maxLen) {
        if (!StringUtils.hasText(text) || text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen);
    }
}
