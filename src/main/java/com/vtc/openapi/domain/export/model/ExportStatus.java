package com.vtc.openapi.domain.export.model;

/**
 * open_export.status 取值。
 */
public final class ExportStatus {

    /** 已预占 exportId，文件组装中 */
    public static final String PENDING = "PENDING";
    public static final String READY = "READY";
    public static final String FAILED = "FAILED";

    private ExportStatus() {
    }
}
