package com.vtc.openapi.domain.export.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.export.model.ExportDataType;
import com.vtc.openapi.domain.export.model.ExportStage;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.infra.converter.InstanceItemConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

@Component
public class MockTaskExportAssembler {

    private static final SimpleDateFormat ISO_UTC;
    private static final String LIVE_PROBE_ORG = "LIVE-PROBE";
    private static final String PORT_SCAN_ORG = "PORT-SCAN";

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
        List<ParsedInstance> parsed = parseInstances(instances);
        TargetRegistry targets = buildTargetRegistry(task, parsed);

        int vulInstanceCount = countVulnerabilityInstances(parsed);

        Map<String, Object> exportMeta = new LinkedHashMap<>();
        exportMeta.put("exportId", exportId);
        exportMeta.put("format", format);
        exportMeta.put("reportTemplateId", task.getReportTemplateId());
        exportMeta.put("exportStage", exportStage);
        exportMeta.put("dataType", dataType);
        exportMeta.put("generatedAt", formatUtc(generatedAt));
        exportMeta.put("expiresAt", formatUtc(expiresAt));
        exportMeta.put("recordCount", vulInstanceCount);
        taskExport.put("export", exportMeta);
        taskExport.put("task", buildTaskNode(task));
        taskExport.put("targets", targets.asList());

        List<Map<String, Object>> liveResults = buildLiveProbeResults(parsed, targets, dataType);
        List<Map<String, Object>> portResults = buildPortScanResults(parsed, targets, dataType);
        List<Map<String, Object>> vulnerabilities = buildVulnerabilities(parsed, targets);

        if (shouldIncludeLiveProbe(dataType, exportStage)) {
            taskExport.put("liveProbeResults", liveResults);
        }
        if (shouldIncludePortScan(dataType, exportStage)) {
            taskExport.put("portScanResults", portResults);
        }
        if (shouldIncludeVulnerabilities(dataType, exportStage)) {
            taskExport.put("vulnerabilities", vulnerabilities);
        }

        taskExport.put("summary", buildSummary(targets, liveResults, portResults, vulInstanceCount));

        root.put("taskExport", taskExport);
        return root;
    }

    private static Map<String, Object> buildTaskNode(OpenTaskDO task) {
        Map<String, Object> taskNode = new LinkedHashMap<>();
        taskNode.put("taskId", task.getTaskId());
        taskNode.put("extTaskId", task.getExtTaskId());
        taskNode.put("taskName", task.getTaskName());
        taskNode.put("targetType", resolveTargetType(task));
        taskNode.put("type", task.getVulnType());
        taskNode.put("scanTemplateId", task.getScanTemplateId());
        taskNode.put("reportTemplateId", task.getReportTemplateId());
        taskNode.put("status", task.getStatus());
        taskNode.put("startedAt", formatUtc(task.getStartedAt()));
        taskNode.put("finishedAt", formatUtc(task.getFinishedAt()));
        return taskNode;
    }

    private static Map<String, Object> buildSummary(TargetRegistry targets,
                                                    List<Map<String, Object>> liveResults,
                                                    List<Map<String, Object>> portResults,
                                                    int vulInstanceCount) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalTargets", targets.size());
        summary.put("aliveTargets", countAlive(liveResults));
        summary.put("openPorts", portResults.size());
        summary.put("totalInstances", vulInstanceCount);
        return summary;
    }

    private static int countAlive(List<Map<String, Object>> liveResults) {
        int count = 0;
        for (Map<String, Object> row : liveResults) {
            Object alive = row.get("alive");
            if (Boolean.TRUE.equals(alive)) {
                count++;
            }
        }
        return count;
    }

    private static boolean shouldIncludeLiveProbe(String dataType, String exportStage) {
        if (ExportStage.VERIFY_FIX_SCAN.equals(exportStage) || ExportStage.VERIFY_SCAN.equals(exportStage)) {
            return true;
        }
        return ExportDataType.LIVE_PROBE.equals(dataType)
                || ExportDataType.PORT_SCAN.equals(dataType)
                || ExportDataType.SYSTEM_VULNERABILITY.equals(dataType);
    }

    private static boolean shouldIncludePortScan(String dataType, String exportStage) {
        if (ExportStage.VERIFY_FIX_SCAN.equals(exportStage)) {
            return true;
        }
        if (ExportStage.VERIFY_SCAN.equals(exportStage)) {
            return false;
        }
        return ExportDataType.PORT_SCAN.equals(dataType)
                || ExportDataType.SYSTEM_VULNERABILITY.equals(dataType);
    }

    private static boolean shouldIncludeVulnerabilities(String dataType, String exportStage) {
        if (ExportStage.VERIFY_FIX_SCAN.equals(exportStage) || ExportStage.VERIFY_SCAN.equals(exportStage)) {
            return ExportDataType.SYSTEM_VULNERABILITY.equals(dataType)
                    || ExportDataType.MIXED.equals(dataType);
        }
        return ExportDataType.SYSTEM_VULNERABILITY.equals(dataType);
    }

    private List<Map<String, Object>> buildLiveProbeResults(List<ParsedInstance> parsed,
                                                            TargetRegistry targets,
                                                            String dataType) {
        List<Map<String, Object>> results = new ArrayList<>();
        Set<String> seenAddresses = new LinkedHashSet<>();
        int seq = 0;

        for (ParsedInstance row : parsed) {
            if (!row.kind.equals(InstanceKind.LIVE_PROBE)) {
                continue;
            }
            String address = row.address();
            if (!StringUtils.hasText(address) || !seenAddresses.add(address)) {
                continue;
            }
            seq++;
            results.add(liveProbeRow(row, targets.targetIdFor(address), seq));
        }

        if (ExportDataType.LIVE_PROBE.equals(dataType)) {
            return results;
        }

        for (ParsedInstance row : parsed) {
            if (row.kind.equals(InstanceKind.LIVE_PROBE)) {
                continue;
            }
            String address = row.address();
            if (!StringUtils.hasText(address) || !seenAddresses.add(address)) {
                continue;
            }
            seq++;
            results.add(derivedLiveProbeRow(address, targets.targetIdFor(address), row, seq));
        }
        return results;
    }

    private List<Map<String, Object>> buildPortScanResults(List<ParsedInstance> parsed,
                                                           TargetRegistry targets,
                                                           String dataType) {
        List<Map<String, Object>> results = new ArrayList<>();
        Set<String> dedupe = new LinkedHashSet<>();
        int seq = 0;

        for (ParsedInstance row : parsed) {
            if (row.kind.equals(InstanceKind.LIVE_PROBE)) {
                continue;
            }
            if (row.kind.equals(InstanceKind.PORT_SCAN) || shouldDerivePortRow(dataType, row)) {
                String key = portDedupeKey(row);
                if (!dedupe.add(key)) {
                    continue;
                }
                seq++;
                results.add(portScanRow(row, targets.targetIdFor(row.address()), seq));
            }
        }
        return results;
    }

    private static boolean shouldDerivePortRow(String dataType, ParsedInstance row) {
        return ExportDataType.SYSTEM_VULNERABILITY.equals(dataType)
                && row.kind.equals(InstanceKind.VULNERABILITY)
                && row.port() != null
                && row.port() > 0;
    }

    private static Map<String, Object> liveProbeRow(ParsedInstance row, String targetId, int seq) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("liveProbeId", "LP-" + seq);
        node.put("targetId", targetId);
        node.put("address", row.address());
        node.put("alive", true);
        node.put("probeMethod", firstOf(row.snap.getString("vulSvc"), "ICMP"));
        node.put("detectedAt", formatTransferTime(row.snap.getString("transferTime")));
        return node;
    }

    private static Map<String, Object> derivedLiveProbeRow(String address, String targetId,
                                                           ParsedInstance source, int seq) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("liveProbeId", "LP-" + seq);
        node.put("targetId", targetId);
        node.put("address", address);
        node.put("alive", true);
        node.put("probeMethod", inferProbeMethod(source));
        node.put("detectedAt", formatTransferTime(source.snap.getString("transferTime")));
        return node;
    }

    private static Map<String, Object> portScanRow(ParsedInstance row, String targetId, int seq) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("portScanId", "PS-" + seq);
        node.put("targetId", targetId);
        node.put("address", row.address());
        node.put("port", row.port());
        node.put("protocol", row.protocol());
        node.put("state", normalizePortState(row.snap.getString("extVulnRef")));
        node.put("service", row.snap.getString("vulSvc"));
        node.put("banner", row.snap.getString("extVulnRef"));
        node.put("detectedAt", formatTransferTime(row.snap.getString("transferTime")));
        return node;
    }

    private List<Map<String, Object>> buildVulnerabilities(List<ParsedInstance> parsed, TargetRegistry targets) {
        Map<String, Map<String, Object>> byVulId = new LinkedHashMap<>();
        for (ParsedInstance row : parsed) {
            if (!row.kind.equals(InstanceKind.VULNERABILITY)) {
                continue;
            }
            JSONObject snap = row.snap;
            String vulId = firstOf(snap.getString("vulID"), snap.getString("vulId"));
            if (!StringUtils.hasText(vulId)) {
                vulId = "UNKNOWN";
            }
            String address = row.address();
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
            inst.put("vulInfoID", row.vulInfoId());
            inst.put("targetId", targets.targetIdFor(address));
            inst.put("vulInfoStat", row.row.getVulInfoStat());
            inst.put("vulNetAddr", address);
            inst.put("vulPort", row.port());
            inst.put("vulSvc", snap.getString("vulSvc"));
            inst.put("isAccess", snap.getInteger("isAccess"));
            inst.put("transferTime", snap.getString("transferTime"));
            inst.put("extVulnRef", snap.getString("extVulnRef"));
            instList.add(inst);
        }
        return new ArrayList<>(byVulId.values());
    }

    private static TargetRegistry buildTargetRegistry(OpenTaskDO task, List<ParsedInstance> parsed) {
        TargetRegistry registry = new TargetRegistry();
        for (String host : parseTaskHosts(task.getTargetsJson())) {
            registry.register(host, inferTargetType(host, task.getTargetType()));
        }
        for (ParsedInstance row : parsed) {
            registry.register(row.address(), inferTargetType(row.address(), task.getTargetType()));
        }
        return registry;
    }

    private static List<String> parseTaskHosts(String targetsJson) {
        List<String> hosts = new ArrayList<>();
        if (!StringUtils.hasText(targetsJson)) {
            return hosts;
        }
        try {
            JSONObject root = JSON.parseObject(targetsJson);
            if (root == null) {
                return hosts;
            }
            String raw = root.getString("hosts");
            if (!StringUtils.hasText(raw)) {
                return hosts;
            }
            for (String part : raw.split("[,;\\s]+")) {
                if (StringUtils.hasText(part)) {
                    hosts.add(part.trim());
                }
            }
        } catch (Exception ignored) {
            // ignore malformed targetsJson
        }
        return hosts;
    }

    private static List<ParsedInstance> parseInstances(List<OpenVulnInstanceDO> instances) {
        List<ParsedInstance> parsed = new ArrayList<>();
        if (instances == null) {
            return parsed;
        }
        for (OpenVulnInstanceDO row : instances) {
            JSONObject snap = parseSnapshot(row);
            if (snap == null) {
                continue;
            }
            parsed.add(new ParsedInstance(row, snap, classify(snap)));
        }
        return parsed;
    }

    private static int countVulnerabilityInstances(List<ParsedInstance> parsed) {
        int count = 0;
        for (ParsedInstance row : parsed) {
            if (row.kind.equals(InstanceKind.VULNERABILITY)) {
                count++;
            }
        }
        return count;
    }

    private static InstanceKind classify(JSONObject snap) {
        String orgVulId = snap.getString("orgVulId");
        if (LIVE_PROBE_ORG.equalsIgnoreCase(orgVulId)
                || containsLiveProbeMarker(snap.getString("extVulnRef"))) {
            return InstanceKind.LIVE_PROBE;
        }
        String vulId = firstOf(snap.getString("vulID"), snap.getString("vulId"));
        if (PORT_SCAN_ORG.equalsIgnoreCase(orgVulId)
                || (StringUtils.hasText(vulId) && vulId.startsWith("PORT-"))) {
            return InstanceKind.PORT_SCAN;
        }
        return InstanceKind.VULNERABILITY;
    }

    private static boolean containsLiveProbeMarker(String extVulnRef) {
        return StringUtils.hasText(extVulnRef)
                && extVulnRef.toLowerCase(Locale.ROOT).contains("liveprobe=true");
    }

    private static String portDedupeKey(ParsedInstance row) {
        return row.address() + "|" + row.port() + "|" + row.protocol();
    }

    private static String normalizePortState(String extVulnRef) {
        if (!StringUtils.hasText(extVulnRef)) {
            return "open";
        }
        String lower = extVulnRef.toLowerCase(Locale.ROOT);
        if ("open".equals(lower) || "opened".equals(lower)) {
            return "open";
        }
        if (lower.contains("closed")) {
            return "closed";
        }
        if (lower.contains("filtered")) {
            return "filtered";
        }
        return "open";
    }

    private static String inferProbeMethod(ParsedInstance row) {
        if (row.kind.equals(InstanceKind.PORT_SCAN)) {
            return "TCP";
        }
        String svc = row.snap.getString("vulSvc");
        return StringUtils.hasText(svc) ? svc : "ICMP";
    }

    private static String resolveTargetType(OpenTaskDO task) {
        if (StringUtils.hasText(task.getTargetType())) {
            return task.getTargetType();
        }
        return "IPV4";
    }

    private static String inferTargetType(String address, String taskTargetType) {
        if (StringUtils.hasText(taskTargetType)) {
            return taskTargetType;
        }
        if (!StringUtils.hasText(address)) {
            return "IPV4";
        }
        if (address.contains("://") || address.startsWith("http")) {
            return "URL";
        }
        if (address.contains(":")) {
            return address.chars().filter(ch -> ch == ':').count() > 1 ? "IPV6" : "IPV4";
        }
        return "IPV4";
    }

    private static String formatTransferTime(String transferTime) {
        if (!StringUtils.hasText(transferTime)) {
            return null;
        }
        try {
            long epochSec = Long.parseLong(transferTime.trim());
            return formatUtc(new Date(epochSec * 1000L));
        } catch (NumberFormatException ex) {
            return transferTime;
        }
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

    private enum InstanceKind {
        LIVE_PROBE, PORT_SCAN, VULNERABILITY
    }

    private static final class ParsedInstance {
        private final OpenVulnInstanceDO row;
        private final JSONObject snap;
        private final InstanceKind kind;

        private ParsedInstance(OpenVulnInstanceDO row, JSONObject snap, InstanceKind kind) {
            this.row = row;
            this.snap = snap;
            this.kind = kind;
        }

        private String address() {
            return snap.getString("vulNetAddr");
        }

        private Integer port() {
            return snap.getInteger("vulPort");
        }

        private String protocol() {
            String proto = snap.getString("vulTransProto");
            if (StringUtils.hasText(proto)) {
                return proto.toUpperCase(Locale.ROOT);
            }
            String svc = snap.getString("vulSvc");
            if (StringUtils.hasText(svc) && svc.toUpperCase(Locale.ROOT).contains("UDP")) {
                return "UDP";
            }
            return "TCP";
        }

        private String vulInfoId() {
            return firstOf(row.getVulInfoId(), snap.getString("vulInfoID"));
        }
    }

    private static final class TargetRegistry {
        private final Map<String, Map<String, Object>> byAddress = new LinkedHashMap<>();
        private int seq = 0;

        private void register(String address, String targetType) {
            if (!StringUtils.hasText(address) || byAddress.containsKey(address)) {
                return;
            }
            seq++;
            Map<String, Object> target = new LinkedHashMap<>();
            target.put("targetId", "TGT-" + seq);
            target.put("target", address);
            target.put("targetType", targetType);
            byAddress.put(address, target);
        }

        private String targetIdFor(String address) {
            Map<String, Object> target = byAddress.get(address);
            return target != null ? (String) target.get("targetId") : null;
        }

        private int size() {
            return byAddress.size();
        }

        private List<Map<String, Object>> asList() {
            return new ArrayList<>(byAddress.values());
        }
    }
}
