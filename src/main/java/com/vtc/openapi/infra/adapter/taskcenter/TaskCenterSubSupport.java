package com.vtc.openapi.infra.adapter.taskcenter;

public final class TaskCenterSubSupport {

    public static final int PHASE_SURVEY = 1;
    public static final int PHASE_VERIFY = 2;
    /** 修复核验复扫（不经排查/验证编排） */
    public static final int PHASE_VERIFY_FIX = 3;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_FINISHED = "FINISHED";
    public static final String STATUS_FAILED = "FAILED";

    /** 原始报告归档状态：已完成扫描，等待 vuln-task-center 推送报告路径 */
    public static final String REPORT_WAITING_PATH = "WAITING_PATH";
    /** 原始报告归档状态：已收到报告路径，待下载归档 */
    public static final String REPORT_PENDING = "PENDING";
    /** 原始报告归档状态：已下载并归档至文件服务 */
    public static final String REPORT_ARCHIVED = "ARCHIVED";
    /** 原始报告归档状态：归档失败，可手动重试 */
    public static final String REPORT_FAILED = "FAILED";

    /** survey state=2 表示已完成 */
    public static final String SURVEY_STATE_FINISHED = "2";

    private TaskCenterSubSupport() {
    }
}
