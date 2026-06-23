package com.vtc.openapi.infra.export;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.infra.adapter.taskcenter.TaskCenterVerifyMergeService;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 外发前按「系统漏洞指纹」归并实例：双扫等同 IP/端口/协议/服务/厂商漏洞 ID 的多条 open_vuln_instance 合并为一条。
 */
@Component
public class ExportInstanceDeduper {

    private static final String LIVE_PROBE_ORG = "LIVE-PROBE";
    private static final String PORT_SCAN_ORG = "PORT-SCAN";

    private final TaskCenterVerifyMergeService mergeService;

    public ExportInstanceDeduper(TaskCenterVerifyMergeService mergeService) {
        this.mergeService = mergeService;
    }

    public List<OpenVulnInstanceDO> dedupeSystemVulnerabilities(List<OpenVulnInstanceDO> instances) {
        if (CollectionUtils.isEmpty(instances)) {
            return Collections.emptyList();
        }
        List<OpenVulnInstanceDO> passthrough = new ArrayList<>();
        Map<String, List<OpenVulnInstanceDO>> grouped = new LinkedHashMap<>();
        for (OpenVulnInstanceDO row : instances) {
            if (row == null) {
                continue;
            }
            JSONObject snap = parseSnapshot(row);
            if (snap == null || !isSystemVulnerability(snap)) {
                passthrough.add(row);
                continue;
            }
            grouped.computeIfAbsent(fingerprint(snap), key -> new ArrayList<>()).add(row);
        }
        if (grouped.isEmpty()) {
            return instances;
        }
        List<OpenVulnInstanceDO> result = new ArrayList<>(passthrough);
        for (List<OpenVulnInstanceDO> group : grouped.values()) {
            result.add(collapseGroup(group));
        }
        return result;
    }

    public String fingerprint(JSONObject snap) {
        return mergeService.dedupKey(snap);
    }

    public String fingerprint(OpenVulnInstanceDO row) {
        JSONObject snap = parseSnapshot(row);
        return snap != null ? fingerprint(snap) : row.getVulInfoId();
    }

    private OpenVulnInstanceDO collapseGroup(List<OpenVulnInstanceDO> group) {
        if (group.size() == 1) {
            return group.get(0);
        }
        OpenVulnInstanceDO winner = pickWinner(group);
        List<JSONObject> snaps = new ArrayList<>();
        for (OpenVulnInstanceDO row : group) {
            JSONObject snap = parseSnapshot(row);
            if (snap != null) {
                snaps.add(snap);
            }
        }
        JSONObject mergedSnap = snaps.get(0);
        if (snaps.size() > 1) {
            List<List<JSONObject>> perScanner = new ArrayList<>();
            for (JSONObject snap : snaps) {
                perScanner.add(Collections.singletonList(snap));
            }
            List<JSONObject> merged = mergeService.mergeUnion(perScanner);
            if (!merged.isEmpty()) {
                mergedSnap = merged.get(0);
            }
        }
        OpenVulnInstanceDO collapsed = copyRow(winner);
        collapsed.setVulInfoStat(resolveMergedStat(group));
        if (mergedSnap != null) {
            mergedSnap.put("vulInfoStat", collapsed.getVulInfoStat());
            collapsed.setSnapshotJson(mergedSnap.toJSONString());
        }
        return collapsed;
    }

    private static OpenVulnInstanceDO pickWinner(List<OpenVulnInstanceDO> group) {
        OpenVulnInstanceDO winner = group.get(0);
        for (int i = 1; i < group.size(); i++) {
            OpenVulnInstanceDO candidate = group.get(i);
            if (compareStat(candidate.getVulInfoStat(), winner.getVulInfoStat()) > 0) {
                winner = candidate;
            }
        }
        return winner;
    }

    private static int resolveMergedStat(List<OpenVulnInstanceDO> group) {
        Integer best = null;
        for (OpenVulnInstanceDO row : group) {
            if (row.getVulInfoStat() == null) {
                continue;
            }
            if (best == null || compareStat(row.getVulInfoStat(), best) > 0) {
                best = row.getVulInfoStat();
            }
        }
        return best != null ? best : group.get(0).getVulInfoStat();
    }

    /** 外发归并：优先保留交叉合并后的较高状态（如 3 &gt; 2 &gt; 1）。 */
    private static int compareStat(Integer a, Integer b) {
        if (a == null) {
            return b == null ? 0 : -1;
        }
        if (b == null) {
            return 1;
        }
        return Integer.compare(a, b);
    }

    private static OpenVulnInstanceDO copyRow(OpenVulnInstanceDO source) {
        OpenVulnInstanceDO row = new OpenVulnInstanceDO();
        row.setId(source.getId());
        row.setPartnerId(source.getPartnerId());
        row.setVulInfoId(source.getVulInfoId());
        row.setVulnDisposalId(source.getVulnDisposalId());
        row.setEngineTaskId(source.getEngineTaskId());
        row.setTaskId(source.getTaskId());
        row.setExtTaskId(source.getExtTaskId());
        row.setScanTemplateId(source.getScanTemplateId());
        row.setReportTemplateId(source.getReportTemplateId());
        row.setBundleId(source.getBundleId());
        row.setIngestStatus(source.getIngestStatus());
        row.setIngestAt(source.getIngestAt());
        row.setVulInfoStat(source.getVulInfoStat());
        row.setSnapshotJson(source.getSnapshotJson());
        row.setCreatedAt(source.getCreatedAt());
        row.setUpdatedAt(source.getUpdatedAt());
        return row;
    }

    private static JSONObject parseSnapshot(OpenVulnInstanceDO row) {
        if (row == null || !StringUtils.hasText(row.getSnapshotJson())) {
            return null;
        }
        try {
            return JSON.parseObject(row.getSnapshotJson());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isSystemVulnerability(JSONObject snap) {
        String orgVulId = snap.getString("orgVulId");
        if (LIVE_PROBE_ORG.equalsIgnoreCase(orgVulId) || PORT_SCAN_ORG.equalsIgnoreCase(orgVulId)) {
            return false;
        }
        String vulId = firstOf(snap.getString("vulID"), snap.getString("vulId"));
        return !StringUtils.hasText(vulId) || !vulId.startsWith("PORT-");
    }

    private static String firstOf(String a, String b) {
        return StringUtils.hasText(a) ? a : b;
    }
}
