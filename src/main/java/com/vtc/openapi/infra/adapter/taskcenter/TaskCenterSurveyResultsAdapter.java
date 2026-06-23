package com.vtc.openapi.infra.adapter.taskcenter;

import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.infra.feign.dto.taskcenter.TaskCenterSurveyBundle;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TaskCenterSurveyResultsAdapter {

    public List<JSONObject> toVulnInstances(TaskCenterSurveyBundle bundle) {
        List<JSONObject> list = new ArrayList<>();
        if (bundle == null || CollectionUtils.isEmpty(bundle.getVulnScanResultList())) {
            return list;
        }
        Map<String, Map<String, Object>> dbIndex = indexVulnDatabase(bundle.getVulnDatabaseList());
        for (Map<String, Object> row : bundle.getVulnScanResultList()) {
            JSONObject item = mapVulnRow(row, dbIndex);
            if (item != null) {
                list.add(item);
            }
        }
        return list;
    }

    private JSONObject mapVulnRow(Map<String, Object> row, Map<String, Map<String, Object>> dbIndex) {
        if (row == null) {
            return null;
        }
        String classify = stringVal(row.get("classify"));
        String vulId = stringVal(row.get("vulId"));
        Map<String, Object> db = dbIndex.get(classify + "," + vulId);

        JSONObject result = new JSONObject();
        result.put("vulId", vulId);
        String cve = firstNonBlank(stringVal(row.get("cve")),
                db != null ? stringVal(db.get("cve")) : null);
        result.put("orgVulId", firstNonBlank(cve, vulId));
        result.put("vulName", firstNonBlank(stringVal(row.get("vulnName")),
                stringVal(db != null ? db.get("vulnName") : null)));
        result.put("vulDesc", firstNonBlank(stringVal(row.get("messString")),
                stringVal(db != null ? db.get("description") : null)));
        result.put("vulLevel", mapLevel(stringVal(row.get("level"))));
        result.put("vulInfoStat", 1);
        result.put("vulNetAddr", stringVal(row.get("ip")));
        result.put("vulPort", parsePort(stringVal(row.get("port"))));
        result.put("vulTransProto", stringVal(row.get("protocol")));
        result.put("vulSvc", stringVal(row.get("service")));
        result.put("engHash", classify);
        result.put("cve", stringVal(row.get("cve")));
        return result;
    }

    private static Map<String, Map<String, Object>> indexVulnDatabase(List<Map<String, Object>> dbList) {
        Map<String, Map<String, Object>> index = new HashMap<>();
        if (CollectionUtils.isEmpty(dbList)) {
            return index;
        }
        for (Map<String, Object> db : dbList) {
            if (db == null) {
                continue;
            }
            String vulId = stringVal(db.get("vulId"));
            String classify = stringVal(db.get("classify"));
            if (StringUtils.hasText(vulId)) {
                index.put((classify != null ? classify : "") + "," + vulId, db);
            }
        }
        return index;
    }

    private static Integer mapLevel(String level) {
        if (level == null) {
            return 2;
        }
        switch (level.toLowerCase()) {
            case "urgent":
                return 4;
            case "high":
                return 3;
            case "medium":
                return 2;
            case "low":
                return 1;
            case "info":
                return 0;
            default:
                return 2;
        }
    }

    private static Integer parsePort(String port) {
        if (!StringUtils.hasText(port)) {
            return null;
        }
        try {
            return Integer.parseInt(port.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String stringVal(Object value) {
        return value != null ? value.toString() : null;
    }

    private static String firstNonBlank(String a, String b) {
        return StringUtils.hasText(a) ? a : b;
    }
}
