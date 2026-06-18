package com.vtc.openapi.domain.instance.model.audit;

import java.util.function.Supplier;

/**
 * ThreadLocal 传递状态跃迁审计元数据至 {@link com.vtc.openapi.infra.repository.OpenVulnInstanceRepositoryImpl}。
 */
public final class OpenVulnInstanceAuditContext {

    private static final ThreadLocal<OpenVulnInstanceAudit> HOLDER = new ThreadLocal<>();

    private OpenVulnInstanceAuditContext() {
    }

    public static void set(OpenVulnInstanceAudit audit) {
        HOLDER.set(audit);
    }

    public static OpenVulnInstanceAudit get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static void runWith(OpenVulnInstanceAudit audit, Runnable action) {
        set(audit);
        try {
            action.run();
        } finally {
            clear();
        }
    }

    public static <T> T callWith(OpenVulnInstanceAudit audit, Supplier<T> action) {
        set(audit);
        try {
            return action.get();
        } finally {
            clear();
        }
    }
}
