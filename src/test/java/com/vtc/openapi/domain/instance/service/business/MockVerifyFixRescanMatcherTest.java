package com.vtc.openapi.domain.instance.service.business;

import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.instance.model.support.VerifyFixRescanFingerprint;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MockVerifyFixRescanMatcherTest {

    private final MockVerifyFixRescanMatcher matcher = new MockVerifyFixRescanMatcher();

    @Test
    public void buildFingerprintKeys_fromRescanRows() {
        JSONObject row = new JSONObject();
        row.put("vulID", "VUL-001");
        row.put("vulNetAddr", "10.0.0.1");
        row.put("vulPort", 443);
        Set<String> keys = matcher.buildFingerprintKeys(Arrays.asList(row));
        assertEquals(1, keys.size());
        assertTrue(keys.contains(VerifyFixRescanFingerprint.of("VUL-001", "10.0.0.1", 443).key()));
    }

    @Test
    public void isStillPresent_whenFingerprintInReport() {
        Set<String> keys = new HashSet<>();
        keys.add(VerifyFixRescanFingerprint.of("VUL-001", "10.0.0.1", 80).key());
        com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO inst =
                new com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO();
        inst.setSnapshotJson("{\"vulID\":\"VUL-001\",\"vulNetAddr\":\"10.0.0.1\",\"vulPort\":80}");
        assertTrue(matcher.isStillPresent(inst, keys));
    }

    @Test
    public void isStillPresent_whenNotInReport() {
        Set<String> keys = new HashSet<>();
        keys.add(VerifyFixRescanFingerprint.of("VUL-999", "10.0.0.2", 80).key());
        com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO inst =
                new com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO();
        inst.setSnapshotJson("{\"vulID\":\"VUL-001\",\"vulNetAddr\":\"10.0.0.1\",\"vulPort\":80}");
        assertFalse(matcher.isStillPresent(inst, keys));
    }
}
