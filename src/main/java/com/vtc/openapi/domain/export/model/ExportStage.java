package com.vtc.openapi.domain.export.model;

public final class ExportStage {

    public static final String TASK_COMPLETED = "TASK_COMPLETED";
    public static final String VERIFY_SCAN = "VERIFY_SCAN";
    public static final String VERIFY_FIX_SCAN = "VERIFY_FIX_SCAN";
    /** 原始扫描报告归档（按子任务） */
    public static final String RAW_SCAN_ARCHIVE = "RAW_SCAN_ARCHIVE";

    private ExportStage() {
    }
}
