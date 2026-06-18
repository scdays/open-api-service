package com.vtc.openapi.infra.adapter.taskcenter;

/**
 * 开放平台 scanTemplateId / type → task-center taskType。
 */
public final class TaskCenterTaskTypeMapper {

    private TaskCenterTaskTypeMapper() {
    }

    public static String resolveTaskType(Integer vulnType, Integer scanTemplateId) {
        if (scanTemplateId != null) {
            if (scanTemplateId == 1002) {
                return "alive";
            }
            if (scanTemplateId == 1003) {
                return "port";
            }
        }
        if (vulnType != null && vulnType == 3) {
            return "alive";
        }
        return "vuln";
    }
}
