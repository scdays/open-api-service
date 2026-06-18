package com.vtc.openapi.infra.adapter.taskcenter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskScanResultDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.infra.feign.dto.taskcenter.TaskCenterSurveyBundle;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * 将 VTC 原始结果转为 TaskExport §5.6.5 行结构并封装为 {@link OpenTaskScanResultDO}。
 */
@Component
public class TaskCenterExportRowBuilder {

    private static final SimpleDateFormat ISO_UTC;

    static {
        ISO_UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        ISO_UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final TaskCenterLiveProbeMerger liveProbeMerger;

    public TaskCenterExportRowBuilder(TaskCenterLiveProbeMerger liveProbeMerger) {
        this.liveProbeMerger = liveProbeMerger;
    }

    public List<OpenTaskScanResultDO> buildPersistRows(OpenTaskDO task,
                                                       OpenTaskSubDO sub,
                                                       TaskCenterSurveyBundle bundle,
                                                       List<String> taskHosts,
                                                       Date detectedAt) {
        List<OpenTaskScanResultDO> rows = new ArrayList<>();
        if (task == null || sub == null) {
            return rows;
        }
        rows.addAll(buildLiveRows(task, sub, bundle, taskHosts, detectedAt));
        rows.addAll(buildPortRows(task, sub, bundle, detectedAt));
        rows.addAll(buildVulnRows(task, sub, bundle));
        rows.addAll(buildVulnDatabaseRows(task, sub, bundle));
        return rows;
    }

    public List<Map<String, Object>> toLiveExportMaps(List<OpenTaskScanResultDO> rows) {
        return toExportMaps(rows, OpenTaskScanResultDO.TYPE_LIVE_PROBE);
    }

    public List<Map<String, Object>> toPortExportMaps(List<OpenTaskScanResultDO> rows) {
        return toExportMaps(rows, OpenTaskScanResultDO.TYPE_PORT_SCAN);
    }

    private List<OpenTaskScanResultDO> buildLiveRows(OpenTaskDO task,
                                                     OpenTaskSubDO sub,
                                                     TaskCenterSurveyBundle bundle,
                                                     List<String> taskHosts,
                                                     Date detectedAt) {
        List<OpenTaskScanResultDO> rows = new ArrayList<>();
        TaskCenterLiveProbeMerger.MergeInput input = new TaskCenterLiveProbeMerger.MergeInput();
        if (bundle != null) {
            input.setSuccessIps(bundle.getSuccessIps());
            input.setFailIps(bundle.getFailIps());
            input.setSuccessQueryOk(bundle.isSuccessIpsQueryOk());
            input.setFailQueryOk(bundle.isFailIpsQueryOk());
        }
        input.setTaskHosts(taskHosts);
        Map<String, Boolean> merged = liveProbeMerger.merge(input);
        String detected = formatUtc(detectedAt);
        int seq = 0;
        for (Map.Entry<String, Boolean> entry : merged.entrySet()) {
            seq++;
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("liveProbeId", "LP-" + sub.getSubId() + "-" + seq);
            payload.put("targetId", targetIdFor(entry.getKey()));
            payload.put("address", entry.getKey());
            payload.put("alive", entry.getValue());
            payload.put("probeMethod", "ICMP");
            payload.put("detectedAt", detected);

            OpenTaskScanResultDO row = baseRow(task, sub, OpenTaskScanResultDO.TYPE_LIVE_PROBE, entry.getKey());
            row.setPayloadJson(JSON.toJSONString(payload));
            rows.add(row);
        }
        return rows;
    }

    private List<OpenTaskScanResultDO> buildVulnRows(OpenTaskDO task,
                                                    OpenTaskSubDO sub,
                                                    TaskCenterSurveyBundle bundle) {
        List<OpenTaskScanResultDO> rows = new ArrayList<>();
        if (bundle == null || CollectionUtils.isEmpty(bundle.getVulnScanResultList())) {
            return rows;
        }
        for (Map<String, Object> vuln : bundle.getVulnScanResultList()) {
            if (vuln == null) {
                continue;
            }
            String resultKey = resolveVulnResultKey(vuln);
            OpenTaskScanResultDO row = baseRow(task, sub, OpenTaskScanResultDO.TYPE_VULN_SCAN, resultKey);
            row.setPayloadJson(JSON.toJSONString(vuln));
            rows.add(row);
        }
        return rows;
    }

    private List<OpenTaskScanResultDO> buildVulnDatabaseRows(OpenTaskDO task,
                                                             OpenTaskSubDO sub,
                                                             TaskCenterSurveyBundle bundle) {
        if (bundle == null || CollectionUtils.isEmpty(bundle.getVulnDatabaseList())) {
            return Collections.emptyList();
        }
        OpenTaskScanResultDO row = baseRow(
                task, sub, OpenTaskScanResultDO.TYPE_VULN_DATABASE, OpenTaskScanResultDO.VULN_DATABASE_META_KEY);
        row.setPayloadJson(JSON.toJSONString(bundle.getVulnDatabaseList()));
        return Collections.singletonList(row);
    }

    private static String resolveVulnResultKey(Map<String, Object> vuln) {
        String id = stringVal(vuln.get("id"));
        if (StringUtils.hasText(id)) {
            return id;
        }
        String ip = stringVal(vuln.get("ip"));
        String port = stringVal(vuln.get("port"));
        String vulId = stringVal(vuln.get("vulId"));
        return (ip != null ? ip : "") + "|" + (port != null ? port : "") + "|" + (vulId != null ? vulId : "");
    }

    @SuppressWarnings("unchecked")
    private List<OpenTaskScanResultDO> buildPortRows(OpenTaskDO task,
                                                     OpenTaskSubDO sub,
                                                     TaskCenterSurveyBundle bundle,
                                                     Date detectedAt) {
        List<OpenTaskScanResultDO> rows = new ArrayList<>();
        if (bundle == null || CollectionUtils.isEmpty(bundle.getPortScanRows())) {
            return rows;
        }
        String detected = formatUtc(detectedAt);
        int seq = 0;
        for (Map<String, Object> hostRow : bundle.getPortScanRows()) {
            if (hostRow == null) {
                continue;
            }
            String address = stringVal(hostRow.get("ip"));
            if (!StringUtils.hasText(address)) {
                continue;
            }
            String osName = stringVal(hostRow.get("osName"));
            Object portArrayObj = hostRow.get("portInfoArray");
            if (!(portArrayObj instanceof List)) {
                continue;
            }
            for (Object portObj : (List<?>) portArrayObj) {
                if (!(portObj instanceof Map)) {
                    continue;
                }
                Map<String, Object> portInfo = (Map<String, Object>) portObj;
                Integer port = parsePort(stringVal(portInfo.get("port")));
                if (port == null) {
                    continue;
                }
                seq++;
                String protocol = firstNonBlank(stringVal(portInfo.get("protocol")), "tcp");
                String resultKey = address.toLowerCase() + "|" + port + "|" + protocol.toLowerCase();

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("portScanId", "PS-" + sub.getSubId() + "-" + seq);
                payload.put("targetId", targetIdFor(address));
                payload.put("address", address);
                payload.put("port", port);
                payload.put("protocol", protocol.toUpperCase(Locale.ROOT));
                payload.put("state", firstNonBlank(stringVal(portInfo.get("state")), "open"));
                payload.put("service", stringVal(portInfo.get("service")));
                payload.put("banner", stringVal(portInfo.get("banner")));
                payload.put("version", stringVal(portInfo.get("version")));
                payload.put("detectedAt", detected);
                if (StringUtils.hasText(osName)) {
                    payload.put("osName", osName);
                }

                OpenTaskScanResultDO row = baseRow(task, sub, OpenTaskScanResultDO.TYPE_PORT_SCAN, resultKey);
                row.setPayloadJson(JSON.toJSONString(payload));
                rows.add(row);
            }
        }
        return rows;
    }

    private static OpenTaskScanResultDO baseRow(OpenTaskDO task,
                                                OpenTaskSubDO sub,
                                                String resultType,
                                                String resultKey) {
        OpenTaskScanResultDO row = new OpenTaskScanResultDO();
        row.setTaskId(task.getTaskId());
        row.setSubId(sub.getSubId());
        row.setPartnerId(task.getPartnerId());
        row.setScanPhase(sub.getScanPhase());
        row.setSurveyId(sub.getSurveyId());
        row.setScannerType(sub.getScannerType());
        row.setResultType(resultType);
        row.setResultKey(resultKey);
        Date now = new Date();
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        return row;
    }

    private static List<Map<String, Object>> toExportMaps(List<OpenTaskScanResultDO> rows, String type) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (CollectionUtils.isEmpty(rows)) {
            return list;
        }
        for (OpenTaskScanResultDO row : rows) {
            if (row == null || !type.equals(row.getResultType()) || !StringUtils.hasText(row.getPayloadJson())) {
                continue;
            }
            JSONObject json = JSON.parseObject(row.getPayloadJson());
            if (json != null) {
                list.add(new LinkedHashMap<>(json));
            }
        }
        return list;
    }

    public static List<String> parseTaskHosts(String targetsJson) {
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

    private static String targetIdFor(String address) {
        return "TGT-" + Math.abs(address != null ? address.hashCode() : 0);
    }

    private static String formatUtc(Date date) {
        return date != null ? ISO_UTC.format(date) : null;
    }

    private static String stringVal(Object value) {
        return value != null ? value.toString().trim() : null;
    }

    private static String firstNonBlank(String a, String b) {
        return StringUtils.hasText(a) ? a : b;
    }

    private static Integer parsePort(String port) {
        if (!StringUtils.hasText(port)) {
            return null;
        }
        try {
            return Integer.parseInt(port.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
