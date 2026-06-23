package com.vtc.openapi.infra.adapter.taskcenter;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 多扫描器结果合并（SOC UNION / INTERSECT）。
 */
@Component
public class TaskCenterVerifyMergeService {

    public List<JSONObject> mergeUnion(List<List<JSONObject>> perScanner) {
        return merge(perScanner, true);
    }

    public List<JSONObject> merge(List<List<JSONObject>> perScanner, boolean union) {
        if (perScanner == null || perScanner.isEmpty()) {
            return new ArrayList<>();
        }
        if (perScanner.size() == 1) {
            return perScanner.get(0) != null ? perScanner.get(0) : new ArrayList<>();
        }
        List<Map<String, JSONObject>> maps = perScanner.stream()
                .map(list -> list.stream().collect(Collectors.toMap(
                        this::dedupKey, Function.identity(), (a, b) -> a, LinkedHashMap::new)))
                .collect(Collectors.toList());

        if (union) {
            Set<String> allKeys = maps.stream()
                    .flatMap(m -> m.keySet().stream())
                    .collect(Collectors.toSet());
            List<JSONObject> merged = new ArrayList<>();
            for (String key : allKeys) {
                List<JSONObject> hits = maps.stream()
                        .filter(m -> m.containsKey(key))
                        .map(m -> m.get(key))
                        .collect(Collectors.toList());
                merged.add(mergeConflict(hits));
            }
            return merged;
        }
        Set<String> commonKeys = new HashSet<>(maps.get(0).keySet());
        for (int i = 1; i < maps.size(); i++) {
            commonKeys.retainAll(maps.get(i).keySet());
        }
        return commonKeys.stream()
                .map(key -> maps.get(0).get(key))
                .collect(Collectors.toList());
    }

    public Map<String, Integer> countScannerHits(List<List<JSONObject>> perScanner) {
        Map<String, Integer> hitCount = new HashMap<>();
        if (perScanner == null) {
            return hitCount;
        }
        for (List<JSONObject> list : perScanner) {
            if (list == null) {
                continue;
            }
            Set<String> keysInScanner = list.stream().map(this::dedupKey).collect(Collectors.toSet());
            for (String key : keysInScanner) {
                hitCount.merge(key, 1, Integer::sum);
            }
        }
        return hitCount;
    }

    public String dedupKey(JSONObject row) {
        return concat(
                row.getString("vulNetAddr"),
                row.get("vulPort"),
                row.getString("vulTransProto"),
                row.getString("vulSvc"),
                row.getString("vulId"));
    }

    private JSONObject mergeConflict(List<JSONObject> hits) {
        if (hits.size() == 1) {
            return hits.get(0);
        }
        JSONObject a = hits.get(0);
        JSONObject b = hits.get(1);
        JSONObject merged = JSONObject.parseObject(a.toJSONString());
        merged.put("cve", firstNonBlank(a.getString("cve"), b.getString("cve")));
        merged.put("orgVulId", firstNonBlank(
                firstNonBlank(a.getString("cve"), b.getString("cve")),
                firstNonBlank(a.getString("orgVulId"), b.getString("orgVulId"))));
        merged.put("vulName", firstNonBlank(a.getString("vulName"), b.getString("vulName")));
        merged.put("vulDesc", firstNonBlank(a.getString("vulDesc"), b.getString("vulDesc")));
        merged.put("engHash", "MULTI");
        int levelA = a.getInteger("vulLevel") != null ? a.getInteger("vulLevel") : 0;
        int levelB = b.getInteger("vulLevel") != null ? b.getInteger("vulLevel") : 0;
        merged.put("vulLevel", Math.max(levelA, levelB));
        return merged;
    }

    private static String concat(Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (Object part : parts) {
            sb.append(part != null ? part.toString() : "");
            sb.append('|');
        }
        return sb.toString();
    }

    private static String firstNonBlank(String a, String b) {
        return StringUtils.hasText(a) ? a : b;
    }
}
