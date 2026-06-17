package com.vtc.openapi.domain.instance.model.support;

/**
 * 修复核验复扫结果指纹：vulID + 地址 + 端口。
 */
public final class VerifyFixRescanFingerprint {

    private final String vulId;
    private final String vulNetAddr;
    private final Integer vulPort;

    public VerifyFixRescanFingerprint(String vulId, String vulNetAddr, Integer vulPort) {
        this.vulId = normalize(vulId);
        this.vulNetAddr = normalize(vulNetAddr);
        this.vulPort = vulPort != null ? vulPort : 0;
    }

    public static VerifyFixRescanFingerprint of(String vulId, String vulNetAddr, Integer vulPort) {
        return new VerifyFixRescanFingerprint(vulId, vulNetAddr, vulPort);
    }

    public String key() {
        return vulId + "|" + vulNetAddr + "|" + vulPort;
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
