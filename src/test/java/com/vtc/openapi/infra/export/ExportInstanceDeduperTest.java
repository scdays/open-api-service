package com.vtc.openapi.infra.export;

import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.infra.adapter.taskcenter.TaskCenterVerifyMergeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExportInstanceDeduperTest {

    private ExportInstanceDeduper deduper;

    @BeforeEach
    void setUp() {
        deduper = new ExportInstanceDeduper(new TaskCenterVerifyMergeService());
    }

    @Test
    void mergesDualScannerSameLocationDifferentVulId() {
        OpenVulnInstanceDO rsas = instance("VI-TASK-1-1-VUL-73699-443-0001", 2,
                snapshot("VUL-73699", "10.1.1.1", 443, "https", "php"));
        OpenVulnInstanceDO nessus = instance("VI-TASK-1-7-VUL-88888-443-0002", 1,
                snapshot("VUL-88888", "10.1.1.1", 443, "https", "php"));

        List<OpenVulnInstanceDO> result = deduper.dedupeSystemVulnerabilities(Arrays.asList(rsas, nessus));

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getVulInfoStat());
    }

    @Test
    void mergesDualScannerSameLogicalVulnerability() {
        OpenVulnInstanceDO rsas = instance("VI-TASK-1-1-VUL-73699-443-0001", 2,
                snapshot("VUL-73699", "10.1.1.1", 443, "https", "php"));
        OpenVulnInstanceDO nessus = instance("VI-TASK-1-7-VUL-73699-443-0002", 1,
                snapshot("VUL-73699", "10.1.1.1", 443, "https", "php"));

        List<OpenVulnInstanceDO> result = deduper.dedupeSystemVulnerabilities(Arrays.asList(rsas, nessus));

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getVulInfoStat());
        assertEquals("VI-TASK-1-1-VUL-73699-443-0001", result.get(0).getVulInfoId());
    }

    @Test
    void mergesDifferentVulIdSameLocation() {
        OpenVulnInstanceDO rsas = instance("VI-rsas", 1,
                snapshot("VUL-RSAS", "10.1.1.1", 443, "https", "php"));
        OpenVulnInstanceDO nessus = instance("VI-nessus", 1,
                snapshot("VUL-NESSUS", "10.1.1.1", 443, "https", "php"));

        List<OpenVulnInstanceDO> result = deduper.dedupeSystemVulnerabilities(Arrays.asList(rsas, nessus));

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getVulInfoStat());
    }

    @Test
    void keepsDistinctSameLocationDifferentCve() {
        OpenVulnInstanceDO a = instance("VI-a", 1, snapshot("VUL-A", "10.1.1.1", 443, "https", "php", "CVE-A"));
        OpenVulnInstanceDO b = instance("VI-b", 1, snapshot("VUL-B", "10.1.1.1", 443, "https", "php", "CVE-B"));

        List<OpenVulnInstanceDO> result = deduper.dedupeSystemVulnerabilities(Arrays.asList(a, b));

        assertEquals(2, result.size());
    }

    @Test
    void keepsDistinctVulnerabilities() {
        OpenVulnInstanceDO a = instance("VI-a", 1, snapshot("VUL-A", "10.1.1.1", 443, "https", "php"));
        OpenVulnInstanceDO b = instance("VI-b", 1, snapshot("VUL-B", "10.1.1.1", 80, "http", "nginx"));

        List<OpenVulnInstanceDO> result = deduper.dedupeSystemVulnerabilities(Arrays.asList(a, b));

        assertEquals(2, result.size());
    }

    private static OpenVulnInstanceDO instance(String vulInfoId, int stat, JSONObject snap) {
        OpenVulnInstanceDO row = new OpenVulnInstanceDO();
        row.setVulInfoId(vulInfoId);
        row.setVulInfoStat(stat);
        row.setSnapshotJson(snap.toJSONString());
        return row;
    }

    private static JSONObject snapshot(String vulId, String ip, int port, String proto, String svc) {
        return snapshot(vulId, ip, port, proto, svc, "CVE-TEST");
    }

    private static JSONObject snapshot(String vulId, String ip, int port, String proto, String svc, String cve) {
        JSONObject snap = new JSONObject();
        snap.put("vulId", vulId);
        snap.put("vulNetAddr", ip);
        snap.put("vulPort", port);
        snap.put("vulTransProto", proto);
        snap.put("vulSvc", svc);
        snap.put("cve", cve);
        snap.put("vulName", "test");
        snap.put("vulLevel", 3);
        return snap;
    }
}
