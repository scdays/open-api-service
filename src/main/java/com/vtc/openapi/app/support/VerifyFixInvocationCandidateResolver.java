package com.vtc.openapi.app.support;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.open.OpenApiOperations;
import com.vtc.openapi.domain.open.model.entity.ApiInvocationDO;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 从 Partner verify-fix 调用记录解析 vulInfoID 与 verifyFixJobId。
 */
public final class VerifyFixInvocationCandidateResolver {

    private VerifyFixInvocationCandidateResolver() {
    }

    public static boolean isVerifyFixOperation(String operationId) {
        return OpenApiOperations.VERIFY_FIX_INSTANCE.equals(operationId)
                || OpenApiOperations.VERIFY_FIX_INSTANCE_BATCH.equals(operationId);
    }

    public static List<ExtractedCandidate> extract(ApiInvocationDO invocation) {
        if (invocation == null || !StringUtils.hasText(invocation.getOperationId())) {
            return Collections.emptyList();
        }
        String op = invocation.getOperationId().trim();
        if (OpenApiOperations.VERIFY_FIX_INSTANCE.equals(op)) {
            return extractSingle(invocation);
        }
        if (OpenApiOperations.VERIFY_FIX_INSTANCE_BATCH.equals(op)) {
            return extractBatch(invocation);
        }
        return Collections.emptyList();
    }

    private static List<ExtractedCandidate> extractSingle(ApiInvocationDO invocation) {
        String vulInfoId = firstNonBlank(invocation.getResourceId(), parseRequestVulInfoId(invocation.getRequestBodyJson()));
        if (!StringUtils.hasText(vulInfoId)) {
            return Collections.emptyList();
        }
        ExtractedCandidate row = baseRow(invocation);
        row.vulInfoId = vulInfoId.trim();
        row.verifyFixJobId = parseVerifyFixJobId(invocation.getResponseBodyJson(), vulInfoId);
        return Collections.singletonList(row);
    }

    private static List<ExtractedCandidate> extractBatch(ApiInvocationDO invocation) {
        Map<String, String> vulToJob = parseBatchResponseJobs(invocation.getResponseBodyJson());
        List<String> vulInfoIds = parseBatchRequestVulInfoIds(invocation.getRequestBodyJson());
        if (vulInfoIds.isEmpty() && !vulToJob.isEmpty()) {
            vulInfoIds = new ArrayList<>(vulToJob.keySet());
        }
        if (vulInfoIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<ExtractedCandidate> rows = new ArrayList<>();
        for (String vulInfoId : vulInfoIds) {
            ExtractedCandidate row = baseRow(invocation);
            row.vulInfoId = vulInfoId;
            String jobId = vulToJob.get(vulInfoId);
            if (StringUtils.hasText(jobId)) {
                row.verifyFixJobId = jobId;
            }
            rows.add(row);
        }
        return rows;
    }

    private static ExtractedCandidate baseRow(ApiInvocationDO invocation) {
        ExtractedCandidate row = new ExtractedCandidate();
        row.invocationId = invocation.getInvocationId();
        row.operationId = invocation.getOperationId();
        row.invokedAt = invocation.getStartedAt() != null ? invocation.getStartedAt() : invocation.getFinishedAt();
        return row;
    }

    private static String parseRequestVulInfoId(String requestBodyJson) {
        if (!StringUtils.hasText(requestBodyJson)) {
            return null;
        }
        try {
            JSONObject body = JSON.parseObject(requestBodyJson);
            return firstNonBlank(
                    body.getString("vulInfoID"),
                    body.getString("vulInfoId"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<String> parseBatchRequestVulInfoIds(String requestBodyJson) {
        if (!StringUtils.hasText(requestBodyJson)) {
            return Collections.emptyList();
        }
        try {
            JSONObject body = JSON.parseObject(requestBodyJson);
            JSONArray items = body.getJSONArray("items");
            if (items == null || items.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                JSONObject item = items.getJSONObject(i);
                if (item == null) {
                    continue;
                }
                String vulInfoId = firstNonBlank(item.getString("vulInfoID"), item.getString("vulInfoId"));
                if (StringUtils.hasText(vulInfoId)) {
                    ids.add(vulInfoId.trim());
                }
            }
            return ids;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private static JSONObject unwrapData(String responseBodyJson) {
        if (!StringUtils.hasText(responseBodyJson)) {
            return null;
        }
        try {
            JSONObject root = JSON.parseObject(responseBodyJson);
            if (root == null) {
                return null;
            }
            JSONObject data = root.getJSONObject("data");
            return data != null ? data : root;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Map<String, String> parseBatchResponseJobs(String responseBodyJson) {
        JSONObject body = unwrapData(responseBodyJson);
        if (body == null) {
            return Collections.emptyMap();
        }
        try {
            JSONArray success = body.getJSONArray("success");
            if (success == null || success.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, String> map = new LinkedHashMap<>();
            for (int i = 0; i < success.size(); i++) {
                JSONObject row = success.getJSONObject(i);
                if (row == null) {
                    continue;
                }
                String vulInfoId = firstNonBlank(row.getString("vulInfoID"), row.getString("vulInfoId"));
                String jobId = firstNonBlank(row.getString("verifyFixJobId"), row.getString("verifyFixJobID"));
                if (StringUtils.hasText(vulInfoId) && StringUtils.hasText(jobId)) {
                    map.put(vulInfoId.trim(), jobId.trim());
                }
            }
            return map;
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private static String parseVerifyFixJobId(String responseBodyJson, String vulInfoId) {
        JSONObject body = unwrapData(responseBodyJson);
        if (body == null) {
            return null;
        }
        try {
            JSONArray success = body.getJSONArray("success");
            if (success != null && !success.isEmpty()) {
                for (int i = 0; i < success.size(); i++) {
                    JSONObject row = success.getJSONObject(i);
                    if (row == null) {
                        continue;
                    }
                    String id = firstNonBlank(row.getString("vulInfoID"), row.getString("vulInfoId"));
                    if (vulInfoId.equals(id)) {
                        return firstNonBlank(row.getString("verifyFixJobId"), row.getString("verifyFixJobID"));
                    }
                }
            }
            return firstNonBlank(body.getString("verifyFixJobId"), body.getString("verifyFixJobID"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    public static class ExtractedCandidate {
        public String vulInfoId;
        public String verifyFixJobId;
        public String invocationId;
        public String operationId;
        public java.util.Date invokedAt;
    }
}
