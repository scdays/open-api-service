package com.vtc.openapi.domain.instance.model.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class VulnInstanceFingerprintTest {

    @Test
    void resolveVulIdentity_prefersCve() {
        assertEquals("CVE-2024-1", VulnInstanceFingerprint.resolveVulIdentity("CVE-2024-1", "ORG-1", "name"));
    }

    @Test
    void resolveVulIdentity_usesOrgVulIdWhenCveEmpty() {
        assertEquals("ORG-1", VulnInstanceFingerprint.resolveVulIdentity(null, "ORG-1", "name"));
    }

    @Test
    void resolveVulIdentity_fallsBackToVulName() {
        assertEquals("SQL Injection", VulnInstanceFingerprint.resolveVulIdentity(null, null, "SQL Injection"));
    }

    @Test
    void key_sameLocationDifferentCveAreDistinct() {
        String a = VulnInstanceFingerprint.key("10.0.0.1", 443, "https", "php", "CVE-A");
        String b = VulnInstanceFingerprint.key("10.0.0.1", 443, "https", "php", "CVE-B");
        assertNotEquals(a, b);
    }

    @Test
    void key_sameLocationSameCveMatchesRegardlessOfVulId() {
        com.alibaba.fastjson.JSONObject rsas = new com.alibaba.fastjson.JSONObject();
        rsas.put("vulId", "VUL-RSAS");
        rsas.put("vulNetAddr", "10.0.0.1");
        rsas.put("vulPort", 443);
        rsas.put("vulTransProto", "https");
        rsas.put("vulSvc", "php");
        rsas.put("cve", "CVE-TEST");

        com.alibaba.fastjson.JSONObject nessus = new com.alibaba.fastjson.JSONObject();
        nessus.put("vulId", "VUL-NESSUS");
        nessus.put("vulNetAddr", "10.0.0.1");
        nessus.put("vulPort", 443);
        nessus.put("vulTransProto", "https");
        nessus.put("vulSvc", "php");
        nessus.put("cve", "CVE-TEST");

        assertEquals(VulnInstanceFingerprint.keyFromJson(rsas), VulnInstanceFingerprint.keyFromJson(nessus));
    }
}
