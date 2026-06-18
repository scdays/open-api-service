package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.domain.instance.model.support.VerifyFixRescanFingerprint;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将 vuln-task-center 漏洞扫描结果行映射为修复核验指纹。
 */
@Component
public class TaskCenterVulnFingerprintMapper {

    public Set<String> buildFingerprintKeys(List<Map<String, Object>> vulnRows) {
        Set<String> keys = new HashSet<>();
        if (CollectionUtils.isEmpty(vulnRows)) {
            return keys;
        }
        for (Map<String, Object> row : vulnRows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            keys.add(fromVtcRow(row).key());
        }
        return keys;
    }

    public VerifyFixRescanFingerprint fromVtcRow(Map<String, Object> row) {
        if (row == null) {
            return VerifyFixRescanFingerprint.of(null, null, null);
        }
        String vulId = firstOf(stringVal(row.get("vulId")), stringVal(row.get("cve")));
        String ip = stringVal(row.get("ip"));
        Integer port = parsePort(row.get("port"));
        return VerifyFixRescanFingerprint.of(vulId, ip, port);
    }

    private static Integer parsePort(Object port) {
        if (port == null) {
            return null;
        }
        if (port instanceof Number) {
            return ((Number) port).intValue();
        }
        try {
            return Integer.parseInt(port.toString().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String stringVal(Object value) {
        return value != null ? value.toString().trim() : null;
    }

    private static String firstOf(String a, String b) {
        return StringUtils.hasText(a) ? a : b;
    }
}
