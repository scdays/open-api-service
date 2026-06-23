package com.vtc.openapi.infra.adapter.taskcenter;

import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskCenterVerifyMergeServiceTest {

    private TaskCenterVerifyMergeService mergeService;

    @BeforeEach
    void setUp() {
        mergeService = new TaskCenterVerifyMergeService();
    }

    @Test
    void dedupKey_ignoresVulIdWhenSameCve() {
        JSONObject rsas = row("VUL-RSAS", "10.1.1.1", 443, "https", "php", "CVE-TEST", null, "test");
        JSONObject nessus = row("VUL-NESSUS", "10.1.1.1", 443, "https", "php", "CVE-TEST", null, "other-name");
        assertEquals(mergeService.dedupKey(rsas), mergeService.dedupKey(nessus));
    }

    @Test
    void dedupKey_differsWhenSameLocationDifferentCve() {
        JSONObject a = row("VUL-A", "10.1.1.1", 443, "https", "php", "CVE-A", null, "name");
        JSONObject b = row("VUL-B", "10.1.1.1", 443, "https", "php", "CVE-B", null, "name");
        assertNotEquals(mergeService.dedupKey(a), mergeService.dedupKey(b));
    }

    @Test
    void dedupKey_sameLocationSameVulNameWhenNoCveOrOrgVulId() {
        JSONObject rsas = row("VUL-RSAS", "10.1.1.1", 443, "https", "php", null, null, "SQL Injection");
        JSONObject nessus = row("VUL-NESSUS", "10.1.1.1", 443, "https", "php", null, null, "SQL Injection");
        assertEquals(mergeService.dedupKey(rsas), mergeService.dedupKey(nessus));
    }

    @Test
    void countScannerHits_mergesSameLocationDifferentVulIdSameCve() {
        List<List<JSONObject>> perScanner = Arrays.asList(
                Arrays.asList(row("VUL-A", "10.1.1.1", 443, "https", "php", "CVE-TEST", null, "a")),
                Arrays.asList(row("VUL-B", "10.1.1.1", 443, "https", "php", "CVE-TEST", null, "b")));
        Map<String, Integer> hits = mergeService.countScannerHits(perScanner);
        assertEquals(1, hits.size());
        assertEquals(2, hits.values().iterator().next());
    }

    @Test
    void mergeIntersect_sameLocationDifferentVulIdSameCve() {
        List<List<JSONObject>> perScanner = Arrays.asList(
                Arrays.asList(row("VUL-A", "10.1.1.1", 443, "https", "php", "CVE-TEST", null, "a")),
                Arrays.asList(row("VUL-B", "10.1.1.1", 443, "https", "php", "CVE-TEST", null, "b")));
        List<JSONObject> merged = mergeService.merge(perScanner, false);
        assertEquals(1, merged.size());
    }

    @Test
    void mergeUnion_picksHigherSeverity() {
        JSONObject low = row("VUL-A", "10.1.1.1", 443, "https", "php", "CVE-TEST", null, "a");
        low.put("vulLevel", 2);
        JSONObject high = row("VUL-B", "10.1.1.1", 443, "https", "php", "CVE-TEST", null, "b");
        high.put("vulLevel", 4);
        List<JSONObject> merged = mergeService.mergeUnion(Arrays.asList(
                Arrays.asList(low),
                Arrays.asList(high)));
        assertEquals(1, merged.size());
        assertTrue(merged.get(0).getInteger("vulLevel") >= 4);
    }

    private static JSONObject row(String vulId, String ip, int port, String proto, String svc,
                                  String cve, String orgVulId, String vulName) {
        JSONObject snap = new JSONObject();
        snap.put("vulId", vulId);
        snap.put("vulNetAddr", ip);
        snap.put("vulPort", port);
        snap.put("vulTransProto", proto);
        snap.put("vulSvc", svc);
        if (cve != null) {
            snap.put("cve", cve);
        }
        if (orgVulId != null) {
            snap.put("orgVulId", orgVulId);
        }
        if (vulName != null) {
            snap.put("vulName", vulName);
        }
        return snap;
    }
}
