package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.domain.instance.model.support.VerifyFixRescanFingerprint;
import com.vtc.openapi.domain.instance.model.support.VulnInstanceFingerprint;
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
            return VerifyFixRescanFingerprint.of(null, null, null, null, null);
        }
        String ip = stringVal(row.get("ip"));
        Integer port = parsePort(row.get("port"));
        String protocol = stringVal(row.get("protocol"));
        String service = stringVal(row.get("service"));
        String cve = stringVal(row.get("cve"));
        String orgVulId = stringVal(row.get("orgVulId"));
        String vulName = firstNonBlank(stringVal(row.get("vulnName")), stringVal(row.get("vulName")));
        return VerifyFixRescanFingerprint.of(
                ip, port, protocol, service,
                VulnInstanceFingerprint.resolveVulIdentity(cve, orgVulId, vulName));
    }

    public String keyFromVtcRow(Map<String, Object> row) {
        return VulnInstanceFingerprint.keyFromVtcRow(row);
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

    private static String firstNonBlank(String a, String b) {
        return StringUtils.hasText(a) ? a : b;
    }
}
