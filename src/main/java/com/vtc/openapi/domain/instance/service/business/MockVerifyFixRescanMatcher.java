package com.vtc.openapi.domain.instance.service.business;

import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.instance.model.support.VerifyFixRescanFingerprint;
import com.vtc.openapi.domain.instance.model.support.VulnInstanceFingerprint;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 将复扫报告实例与目标漏洞指纹比对，判定是否仍检出。
 */
@Component
public class MockVerifyFixRescanMatcher {

    public Set<String> buildFingerprintKeys(List<JSONObject> rescanInstances) {
        Set<String> keys = new HashSet<>();
        if (CollectionUtils.isEmpty(rescanInstances)) {
            return keys;
        }
        for (JSONObject row : rescanInstances) {
            if (row == null || !VulnInstanceFingerprint.hasLocation(row)) {
                continue;
            }
            keys.add(VulnInstanceFingerprint.keyFromJson(row));
        }
        return keys;
    }

    public boolean isStillPresent(OpenVulnInstanceDO instance, Set<String> rescanFingerprintKeys) {
        if (instance == null || CollectionUtils.isEmpty(rescanFingerprintKeys)) {
            return false;
        }
        VerifyFixRescanFingerprint target = fingerprintFromInstance(instance);
        return rescanFingerprintKeys.contains(target.key());
    }

    public VerifyFixRescanFingerprint fingerprintFromInstance(OpenVulnInstanceDO instance) {
        if (instance == null || !StringUtils.hasText(instance.getSnapshotJson())) {
            return VerifyFixRescanFingerprint.of(null, null, null, null, null);
        }
        JSONObject snap = JSONObject.parseObject(instance.getSnapshotJson());
        return fingerprintFromJson(snap);
    }

    public VerifyFixRescanFingerprint fingerprintFromJson(JSONObject snap) {
        return VerifyFixRescanFingerprint.fromJson(snap);
    }

    public boolean isVulnerabilityRow(JSONObject snap) {
        if (snap == null) {
            return false;
        }
        String orgVulId = snap.getString("orgVulId");
        if ("LIVE-PROBE".equalsIgnoreCase(orgVulId) || "PORT-SCAN".equalsIgnoreCase(orgVulId)) {
            return false;
        }
        String vulId = firstOf(snap.getString("vulID"), snap.getString("vulId"));
        return StringUtils.hasText(vulId) && !vulId.startsWith("PORT-");
    }

    private static String firstOf(String a, String b) {
        return StringUtils.hasText(a) ? a : b;
    }
}
