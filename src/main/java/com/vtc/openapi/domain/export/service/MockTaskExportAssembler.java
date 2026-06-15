package com.vtc.openapi.domain.export.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.export.model.ExportDataType;
import com.vtc.openapi.domain.export.model.ExportStage;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.infra.converter.InstanceItemConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Component
public class MockTaskExportAssembler {

    private static final SimpleDateFormat ISO_UTC;

    static {
        ISO_UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        ISO_UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public Map<String, Object> assemble(OpenTaskDO task, String exportStage, String format,
                                        List<OpenVulnInstanceDO> instances, String exportId,
                                        Date generatedAt, Date expiresAt) {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> taskExport = new LinkedHashMap<>();

        String dataType = ExportDataType.fromScanTemplateId(task.getScanTemplateId());
        int recordCount = CollectionUtils.isEmpty(instances) ? 0 : instances.size();

        Map<String, Object> exportMeta = new LinkedHashMap<>();
        exportMeta.put("exportId", exportId);
        exportMeta.put("format", format);
        exportMeta.put("reportTemplateId", task.getReportTemplateId());
        exportMeta.put("exportStage", exportStage);
        exportMeta.put("dataType", dataType);
        exportMeta.put("generatedAt", formatUtc(generatedAt));
        exportMeta.put("expiresAt", formatUtc(expiresAt));
        exportMeta.put("recordCount", recordCount);
        taskExport.put("export", exportMeta);

        Map<String, Object> taskNode = new LinkedHashMap<>();
        taskNode.put("taskId", task.getTaskId());
        taskNode.put("extTaskId", task.getExtTaskId());
        taskNode.put("taskName", task.getTaskName());
        taskNode.put("status", task.getStatus());
        taskNode.put("scanTemplateId", task.getScanTemplateId());
        taskExport.put("task", taskNode);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalInstances", recordCount);
        taskExport.put("summary", summary);

        taskExport.put("targets", buildTargets(instances));
        if (ExportStage.VERIFY_FIX_SCAN.equals(exportStage) || ExportDataType.PORT_SCAN.equals(dataType)) {
            taskExport.put("portScanResults", new ArrayList<>());
        }
        if (!ExportDataType.PORT_SCAN.equals(dataType)) {
            taskExport.put("liveProbeResults", new ArrayList<>());
        }
        taskExport.put("vulnerabilities", buildVulnerabilities(instances));

        root.put("taskExport", taskExport);
        return root;
    }

    private List<Map<String, Object>> buildTargets(List<OpenVulnInstanceDO> instances) {
        Map<String, Map<String, Object>> byAddr = new LinkedHashMap<>();
        if (instances == null) {
            return new ArrayList<>();
        }
        int seq = 0;
        for (OpenVulnInstanceDO row : instances) {
            JSONObject snap = parseSnapshot(row);
            String addr = snap != null ? snap.getString("vulNetAddr") : null;
            if (!StringUtils.hasText(addr)) {
                continue;
            }
            if (!byAddr.containsKey(addr)) {
                seq++;
                Map<String, Object> target = new LinkedHashMap<>();
                target.put("targetId", "TGT-" + seq);
                target.put("address", addr);
                byAddr.put(addr, target);
            }
        }
        return new ArrayList<>(byAddr.values());
    }

    private List<Map<String, Object>> buildVulnerabilities(List<OpenVulnInstanceDO> instances) {
        Map<String, Map<String, Object>> byVulId = new LinkedHashMap<>();
        if (instances == null) {
            return new ArrayList<>();
        }
        for (OpenVulnInstanceDO row : instances) {
            JSONObject snap = parseSnapshot(row);
            if (snap == null) {
                continue;
            }
            String vulId = firstOf(snap.getString("vulID"), snap.getString("vulId"));
            if (!StringUtils.hasText(vulId)) {
                vulId = "UNKNOWN";
            }
            Map<String, Object> vul = byVulId.computeIfAbsent(vulId, id -> {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("vulID", id);
                node.put("orgVulId", snap.getString("orgVulId"));
                node.put("vulLevel", snap.getInteger("vulLevel"));
                node.put("vulName", snap.getString("vulName"));
                node.put("instances", new ArrayList<Map<String, Object>>());
                return node;
            });
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> instList = (List<Map<String, Object>>) vul.get("instances");
            Map<String, Object> inst = new LinkedHashMap<>();
            inst.put("vulInfoID", row.getVulInfoId());
            inst.put("vulInfoStat", row.getVulInfoStat());
            inst.put("vulNetAddr", snap.getString("vulNetAddr"));
            inst.put("vulPort", snap.getInteger("vulPort"));
            inst.put("vulSvc", snap.getString("vulSvc"));
            inst.put("isAccess", snap.getInteger("isAccess"));
            inst.put("transferTime", snap.getString("transferTime"));
            inst.put("extVulnRef", snap.getString("extVulnRef"));
            instList.add(inst);
        }
        return new ArrayList<>(byVulId.values());
    }

    private static JSONObject parseSnapshot(OpenVulnInstanceDO row) {
        if (row == null || !StringUtils.hasText(row.getSnapshotJson())) {
            return null;
        }
        try {
            return JSON.parseObject(row.getSnapshotJson());
        } catch (Exception ex) {
            return InstanceItemConverter.fromJson(JSON.parseObject(row.getSnapshotJson())) != null
                    ? JSON.parseObject(row.getSnapshotJson()) : null;
        }
    }

    private static String firstOf(String a, String b) {
        return StringUtils.hasText(a) ? a : b;
    }

    private static String formatUtc(Date date) {
        if (date == null) {
            return null;
        }
        synchronized (ISO_UTC) {
            return ISO_UTC.format(date);
        }
    }
}
