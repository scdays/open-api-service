package com.vtc.openapi.infra.adapter.taskcenter;

public final class TaskCenterSubSupport {

    public static final int PHASE_SURVEY = 1;
    public static final int PHASE_VERIFY = 2;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_FINISHED = "FINISHED";
    public static final String STATUS_FAILED = "FAILED";

    /** survey state=2 表示已完成 */
    public static final String SURVEY_STATE_FINISHED = "2";

    private TaskCenterSubSupport() {
    }
}
