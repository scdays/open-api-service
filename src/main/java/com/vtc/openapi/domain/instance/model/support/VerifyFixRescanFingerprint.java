package com.vtc.openapi.domain.instance.model.support;

import com.alibaba.fastjson.JSONObject;

/**
 * 修复核验复扫结果指纹：IP + 端口 + 传输协议 + 服务 + 漏洞标识（CVE/orgVulId/漏洞名称）。
 */
public final class VerifyFixRescanFingerprint {

    private final String vulNetAddr;
    private final Integer vulPort;
    private final String vulTransProto;
    private final String vulSvc;
    private final String vulIdentity;

    public VerifyFixRescanFingerprint(String vulNetAddr, Integer vulPort, String vulTransProto, String vulSvc,
                                      String vulIdentity) {
        this.vulNetAddr = normalize(vulNetAddr);
        this.vulPort = vulPort != null ? vulPort : 0;
        this.vulTransProto = normalize(vulTransProto);
        this.vulSvc = normalize(vulSvc);
        this.vulIdentity = normalize(vulIdentity);
    }

    public static VerifyFixRescanFingerprint of(String vulNetAddr, Integer vulPort, String vulTransProto, String vulSvc,
                                                String vulIdentity) {
        return new VerifyFixRescanFingerprint(vulNetAddr, vulPort, vulTransProto, vulSvc, vulIdentity);
    }

    public static VerifyFixRescanFingerprint fromJson(JSONObject snap) {
        if (snap == null) {
            return of(null, null, null, null, null);
        }
        return of(
                snap.getString("vulNetAddr"),
                snap.getInteger("vulPort"),
                snap.getString("vulTransProto"),
                snap.getString("vulSvc"),
                VulnInstanceFingerprint.resolveVulIdentity(
                        snap.getString("cve"),
                        snap.getString("orgVulId"),
                        snap.getString("vulName")));
    }

    public String key() {
        return VulnInstanceFingerprint.key(vulNetAddr, vulPort, vulTransProto, vulSvc, vulIdentity);
    }

    public boolean matches(VerifyFixRescanFingerprint other) {
        if (other == null) {
            return false;
        }
        return key().equals(other.key());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
