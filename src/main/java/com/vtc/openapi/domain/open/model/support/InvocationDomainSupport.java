package com.vtc.openapi.domain.open.model.support;

import com.vtc.openapi.domain.open.OpenApiOperations;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 调用记录业务域（domain）解析，与 api_operation.domain / OpenAPI tags 对齐。
 */
public final class InvocationDomainSupport {

    public static final String DOMAIN_TASK = "TASK";
    public static final String DOMAIN_INSTANCE = "INSTANCE";
    public static final String DOMAIN_EXPORT = "EXPORT";
    public static final String DOMAIN_AUTH = "AUTH";

    private static final Set<String> TASK_OPERATIONS = new HashSet<>(Arrays.asList(
            OpenApiOperations.CREATE_TASK,
            OpenApiOperations.CREATE_TASK_BY_JSON,
            OpenApiOperations.CREATE_TASK_BY_FILE,
            OpenApiOperations.CREATE_TASK_BY_UPLOAD,
            OpenApiOperations.LIST_TASKS,
            OpenApiOperations.GET_TASK
    ));

    private static final Set<String> INSTANCE_OPERATIONS = new HashSet<>(Arrays.asList(
            OpenApiOperations.SEARCH_INSTANCES,
            OpenApiOperations.GET_INSTANCE,
            OpenApiOperations.VERIFY_INSTANCE,
            OpenApiOperations.REMEDIATE_INSTANCE,
            OpenApiOperations.VERIFY_FIX_INSTANCE,
            OpenApiOperations.VERIFY_INSTANCE_BATCH,
            OpenApiOperations.REMEDIATE_INSTANCE_BATCH,
            OpenApiOperations.VERIFY_FIX_INSTANCE_BATCH
    ));

    private static final Set<String> EXPORT_OPERATIONS = new HashSet<>(Arrays.asList(
            OpenApiOperations.GET_EXPORT,
            OpenApiOperations.DOWNLOAD_EXPORT,
            OpenApiOperations.LIST_TASK_EXPORTS
    ));

    private static final Set<String> AUTH_OPERATIONS = Collections.singleton("issuePartnerToken");

    private InvocationDomainSupport() {
    }

    public static String resolveDomain(String operationId, String resourceType) {
        if (StringUtils.hasText(resourceType)) {
            if (OpenApiOperations.RESOURCE_TYPE_TASK.equals(resourceType)) {
                return DOMAIN_TASK;
            }
            if (OpenApiOperations.RESOURCE_TYPE_INSTANCE.equals(resourceType)) {
                return DOMAIN_INSTANCE;
            }
            if (OpenApiOperations.RESOURCE_TYPE_EXPORT.equals(resourceType)) {
                return DOMAIN_EXPORT;
            }
        }
        if (!StringUtils.hasText(operationId)) {
            return null;
        }
        if (TASK_OPERATIONS.contains(operationId)) {
            return DOMAIN_TASK;
        }
        if (INSTANCE_OPERATIONS.contains(operationId)) {
            return DOMAIN_INSTANCE;
        }
        if (EXPORT_OPERATIONS.contains(operationId)) {
            return DOMAIN_EXPORT;
        }
        if (AUTH_OPERATIONS.contains(operationId)) {
            return DOMAIN_AUTH;
        }
        return null;
    }

    public static List<String> operationIdsForDomain(String domain) {
        if (!StringUtils.hasText(domain)) {
            return Collections.emptyList();
        }
        switch (domain.trim().toUpperCase(Locale.ROOT)) {
            case DOMAIN_TASK:
                return Arrays.asList(TASK_OPERATIONS.toArray(new String[0]));
            case DOMAIN_INSTANCE:
                return Arrays.asList(INSTANCE_OPERATIONS.toArray(new String[0]));
            case DOMAIN_EXPORT:
                return Arrays.asList(EXPORT_OPERATIONS.toArray(new String[0]));
            case DOMAIN_AUTH:
                return Arrays.asList(AUTH_OPERATIONS.toArray(new String[0]));
            default:
                return Collections.emptyList();
        }
    }
}
