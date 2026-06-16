package com.vtc.openapi.domain.export.service;

import com.alibaba.fastjson.JSON;
import com.vtc.openapi.domain.export.model.ExportStage;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockTaskExportAssemblerTest {

    private MockTaskExportAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new MockTaskExportAssembler();
    }

    @Test
    void liveProbeTaskIncludesLiveResultsOnly() {
        OpenTaskDO task = baseTask(1002, 1);
        List<OpenVulnInstanceDO> instances = List.of(instance(
                "VI-live-1", 1,
                snapshot("LIVE-58", "LIVE-PROBE", "10.65.195.204", 0, "ICMP", "liveProbe=true")));

        Map<String, Object> root = assemble(task, instances);
        Map<String, Object> taskExport = taskExport(root);

        assertCommonSections(taskExport, task);
        assertTrue(taskExport.containsKey("liveProbeResults"));
        assertFalse(taskExport.containsKey("portScanResults"));
        assertFalse(taskExport.containsKey("vulnerabilities"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> live = (List<Map<String, Object>>) taskExport.get("liveProbeResults");
        assertEquals(1, live.size());
        assertEquals("10.65.195.204", live.get(0).get("address"));
        assertEquals(true, live.get(0).get("alive"));
        assertEquals("ICMP", live.get(0).get("probeMethod"));

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) taskExport.get("summary");
        assertEquals(1, summary.get("totalTargets"));
        assertEquals(1, summary.get("aliveTargets"));
        assertEquals(0, summary.get("openPorts"));
        assertEquals(0, summary.get("totalInstances"));
    }

    @Test
    void portScanTaskIncludesLiveAndPortResults() {
        OpenTaskDO task = baseTask(1003, 1);
        List<OpenVulnInstanceDO> instances = List.of(
                instance("VI-port-1", 1, snapshot("PORT-22", "PORT-SCAN", "10.65.195.204", 22, "ssh", "open")),
                instance("VI-port-2", 1, snapshot("PORT-111", "PORT-SCAN", "10.65.195.204", 111, "rpcbind", "open")));

        Map<String, Object> root = assemble(task, instances);
        Map<String, Object> taskExport = taskExport(root);

        assertCommonSections(taskExport, task);
        assertTrue(taskExport.containsKey("liveProbeResults"));
        assertTrue(taskExport.containsKey("portScanResults"));
        assertFalse(taskExport.containsKey("vulnerabilities"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> live = (List<Map<String, Object>>) taskExport.get("liveProbeResults");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ports = (List<Map<String, Object>>) taskExport.get("portScanResults");
        assertEquals(1, live.size());
        assertEquals(2, ports.size());
        assertEquals(22, ports.get(0).get("port"));
        assertEquals("open", ports.get(0).get("state"));

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) taskExport.get("summary");
        assertEquals(1, summary.get("totalTargets"));
        assertEquals(1, summary.get("aliveTargets"));
        assertEquals(2, summary.get("openPorts"));
    }

    @Test
    void vulnerabilityTaskIncludesLivePortAndVulnerabilities() {
        OpenTaskDO task = baseTask(1001, 1);
        List<OpenVulnInstanceDO> instances = List.of(
                instance("VI-vul-1", 1, snapshot("VUL-73699", "CVE-2016-10160",
                        "172.30.3.22", 443, "https", "php/5.3.10")),
                instance("VI-vul-2", 1, snapshot("VUL-73011", "CVE-2007-1888",
                        "172.30.3.22", 443, "https", "php/5.3.10")));

        Map<String, Object> root = assemble(task, instances);
        Map<String, Object> taskExport = taskExport(root);

        assertCommonSections(taskExport, task);
        assertTrue(taskExport.containsKey("liveProbeResults"));
        assertTrue(taskExport.containsKey("portScanResults"));
        assertTrue(taskExport.containsKey("vulnerabilities"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> live = (List<Map<String, Object>>) taskExport.get("liveProbeResults");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ports = (List<Map<String, Object>>) taskExport.get("portScanResults");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> vuls = (List<Map<String, Object>>) taskExport.get("vulnerabilities");
        assertEquals(1, live.size());
        assertEquals(1, ports.size());
        assertEquals(2, vuls.size());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> instList = (List<Map<String, Object>>) vuls.get(0).get("instances");
        assertNotNull(instList.get(0).get("targetId"));

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) taskExport.get("summary");
        assertEquals(2, summary.get("totalInstances"));
        assertEquals(1, summary.get("openPorts"));
    }

    @Test
    void verifyFixScanIncludesAllSectionsForVulnerabilityTask() {
        OpenTaskDO task = baseTask(1001, 1);
        List<OpenVulnInstanceDO> instances = List.of(
                instance("VI-vul-1", 5, snapshot("VUL-1", "CVE-1", "10.0.0.1", 80, "http", "hit")));

        Map<String, Object> root = assembler.assemble(
                task, ExportStage.VERIFY_FIX_SCAN, "json", instances, "EXP-test",
                new Date(), new Date());
        Map<String, Object> taskExport = taskExport(root);

        assertTrue(taskExport.containsKey("liveProbeResults"));
        assertTrue(taskExport.containsKey("portScanResults"));
        assertTrue(taskExport.containsKey("vulnerabilities"));
    }

    @Test
    void verifyScanExcludesPortScanResults() {
        OpenTaskDO task = baseTask(1001, 1);
        List<OpenVulnInstanceDO> instances = List.of(
                instance("VI-vul-1", 1, snapshot("VUL-1", "CVE-1", "10.0.0.1", 443, "https", "hit")));

        Map<String, Object> root = assembler.assemble(
                task, ExportStage.VERIFY_SCAN, "json", instances, "EXP-test",
                new Date(), new Date());
        Map<String, Object> taskExport = taskExport(root);

        assertTrue(taskExport.containsKey("liveProbeResults"));
        assertFalse(taskExport.containsKey("portScanResults"));
        assertTrue(taskExport.containsKey("vulnerabilities"));
    }

    private Map<String, Object> assemble(OpenTaskDO task, List<OpenVulnInstanceDO> instances) {
        return assembler.assemble(task, ExportStage.TASK_COMPLETED, "json", instances,
                "EXP-test", new Date(), new Date());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> taskExport(Map<String, Object> root) {
        return (Map<String, Object>) root.get("taskExport");
    }

    private static void assertCommonSections(Map<String, Object> taskExport, OpenTaskDO task) {
        assertNotNull(taskExport.get("export"));
        assertNotNull(taskExport.get("task"));
        assertNotNull(taskExport.get("summary"));
        assertNotNull(taskExport.get("targets"));

        @SuppressWarnings("unchecked")
        Map<String, Object> taskNode = (Map<String, Object>) taskExport.get("task");
        assertEquals(task.getTaskId(), taskNode.get("taskId"));
        assertEquals(task.getScanTemplateId(), taskNode.get("scanTemplateId"));
        assertEquals(task.getReportTemplateId(), taskNode.get("reportTemplateId"));
        assertEquals("IPV4", taskNode.get("targetType"));
        assertNull(taskNode.get("startedAt"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> targets = (List<Map<String, Object>>) taskExport.get("targets");
        assertFalse(targets.isEmpty());
        assertNotNull(targets.get(0).get("targetId"));
        assertNotNull(targets.get(0).get("target"));
        assertNotNull(targets.get(0).get("targetType"));
    }

    private static OpenTaskDO baseTask(int scanTemplateId, int vulnType) {
        OpenTaskDO task = new OpenTaskDO();
        task.setTaskId("TASK-1");
        task.setExtTaskId("EXT-1");
        task.setTaskName("scan-task");
        task.setTargetType("IPV4");
        task.setVulnType(vulnType);
        task.setScanTemplateId(scanTemplateId);
        task.setReportTemplateId(2001);
        task.setStatus("FINISHED");
        task.setTargetsJson("{\"hosts\":\"10.0.0.1\"}");
        return task;
    }

    private static OpenVulnInstanceDO instance(String vulInfoId, int stat, Map<String, Object> snap) {
        OpenVulnInstanceDO row = new OpenVulnInstanceDO();
        row.setVulInfoId(vulInfoId);
        row.setVulInfoStat(stat);
        row.setSnapshotJson(JSON.toJSONString(snap));
        return row;
    }

    private static Map<String, Object> snapshot(String vulId, String orgVulId, String addr,
                                                int port, String svc, String extRef) {
        Map<String, Object> snap = new java.util.LinkedHashMap<>();
        snap.put("vulID", vulId);
        snap.put("vulId", vulId);
        snap.put("orgVulId", orgVulId);
        snap.put("vulName", "name-" + vulId);
        snap.put("vulLevel", 3);
        snap.put("vulNetAddr", addr);
        snap.put("vulPort", port);
        snap.put("vulSvc", svc);
        snap.put("isAccess", 0);
        snap.put("transferTime", "1589485760");
        snap.put("extVulnRef", extRef);
        return snap;
    }
}
