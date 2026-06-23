package com.vtc.openapi.domain.instance.model.support;

import com.alibaba.fastjson.JSONObject;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 系统漏洞实例比对指纹：IP + 端口 + 传输协议 + 服务 + 漏洞标识。
 * 漏洞标识优先 CVE，其次 orgVulId，二者皆空时取漏洞名称；不含厂商 vulId。
 */
public final class VulnInstanceFingerprint {

    private VulnInstanceFingerprint() {
    }

    public static String keyFromJson(JSONObject row) {
        if (row == null) {
            return key(null, null, null, null, null);
        }
        Integer port = row.getInteger("vulPort");
        if (port == null && row.get("vulPort") != null) {
            try {
                port = Integer.parseInt(row.get("vulPort").toString().trim());
            } catch (NumberFormatException ignored) {
                port = null;
            }
        }
        return key(
                row.getString("vulNetAddr"),
                port,
                row.getString("vulTransProto"),
                row.getString("vulSvc"),
                resolveVulIdentity(
                        row.getString("cve"),
                        row.getString("orgVulId"),
                        row.getString("vulName")));
    }

    public static String keyFromVtcRow(Map<String, Object> row) {
        if (row == null) {
            return key(null, null, null, null, null);
        }
        String cve = stringVal(row.get("cve"));
        String orgVulId = stringVal(row.get("orgVulId"));
        String vulName = firstNonBlank(stringVal(row.get("vulnName")), stringVal(row.get("vulName")));
        return key(
                stringVal(row.get("ip")),
                parsePort(row.get("port")),
                stringVal(row.get("protocol")),
                stringVal(row.get("service")),
                resolveVulIdentity(cve, orgVulId, vulName));
    }

    public static String key(String vulNetAddr, Integer vulPort, String vulTransProto, String vulSvc, String vulIdentity) {
        return concat(
                normalize(vulNetAddr),
                vulPort != null ? vulPort : 0,
                normalize(vulTransProto),
                normalize(vulSvc),
                normalize(vulIdentity));
    }

    /**
     * CVE / orgVulId 非空时优先取用，否则回退漏洞名称。
     */
    public static String resolveVulIdentity(String cve, String orgVulId, String vulName) {
        if (StringUtils.hasText(cve)) {
            return cve.trim();
        }
        if (StringUtils.hasText(orgVulId)) {
            return orgVulId.trim();
        }
        return vulName != null ? vulName.trim() : "";
    }

    public static boolean hasLocation(JSONObject row) {
        if (row == null) {
            return false;
        }
        return StringUtils.hasText(row.getString("vulNetAddr")) || row.get("vulPort") != null;
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

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String concat(Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (Object part : parts) {
            sb.append(part != null ? part.toString() : "");
            sb.append('|');
        }
        return sb.toString();
    }
}
