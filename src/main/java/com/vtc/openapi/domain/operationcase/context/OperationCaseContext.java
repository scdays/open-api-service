package com.vtc.openapi.domain.operationcase.context;

import org.springframework.util.StringUtils;

/**
 * 单次 Partner 写操作链路内传递 caseId（与 {@link com.vtc.openapi.domain.open.model.InvocationContext} 同步）。
 */
public final class OperationCaseContext {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private OperationCaseContext() {
    }

    public static void setCaseId(String caseId) {
        if (StringUtils.hasText(caseId)) {
            HOLDER.set(caseId.trim());
        }
    }

    public static String getCaseId() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
