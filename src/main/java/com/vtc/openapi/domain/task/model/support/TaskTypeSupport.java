package com.vtc.openapi.domain.task.model.support;

import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务类型与扫描目标解析（附录 F / §5.1）。
 */
public final class TaskTypeSupport {

    private TaskTypeSupport() {
    }

    public static void requireValidType(Integer type) {
        if (type == null || type < 1 || type > 3) {
            throw new OpenApiException(OpenApiConstants.CODE_TYPE_INVALID, "type 非法，须为 1/2/3");
        }
    }

    public static String resolveTargetType(int type) {
        switch (type) {
            case 2:
                return "URL";
            case 1:
            case 3:
            default:
                return "IP";
        }
    }

    public static List<String> splitHosts(String hosts) {
        if (!StringUtils.hasText(hosts)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "targets.hosts 不能为空");
        }
        List<String> list = Arrays.stream(hosts.split("[,;]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        if (list.isEmpty()) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "targets.hosts 不能为空");
        }
        return list;
    }

    public static String normalizeProgressStatus(String engineStatus) {
        if (!StringUtils.hasText(engineStatus)) {
            return "PENDING";
        }
        switch (engineStatus.toUpperCase()) {
            case "PENDING":
            case "RUNNING":
            case "FINISHED":
            case "FAILED":
                return engineStatus.toUpperCase();
            case "ACCEPTED":
            case "QUEUED":
                return "PENDING";
            default:
                return engineStatus.toUpperCase();
        }
    }

    public static List<Integer> normalizeTemplateId(Integer templateId) {
        if (templateId == null || templateId <= 0) {
            return Collections.emptyList();
        }
        List<Integer> ids = new ArrayList<>(1);
        ids.add(templateId);
        return ids;
    }
}
