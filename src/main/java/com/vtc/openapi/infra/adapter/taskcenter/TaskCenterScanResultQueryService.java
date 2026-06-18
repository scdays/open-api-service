package com.vtc.openapi.infra.adapter.taskcenter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.task.model.entity.OpenTaskScanResultDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskScanResultRepository;
import com.vtc.openapi.infra.feign.dto.taskcenter.TaskCenterSurveyBundle;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterScanResultQueryService {

    private final IOpenTaskScanResultRepository scanResultRepository;

    public TaskCenterScanResultQueryService(IOpenTaskScanResultRepository scanResultRepository) {
        this.scanResultRepository = scanResultRepository;
    }

    public List<Map<String, Object>> listLiveExportRows(String taskId, int scanPhase) {
        return toMaps(scanResultRepository.listByTaskAndType(
                taskId, scanPhase, OpenTaskScanResultDO.TYPE_LIVE_PROBE));
    }

    public List<Map<String, Object>> listPortExportRows(String taskId, int scanPhase) {
        return toMaps(scanResultRepository.listByTaskAndType(
                taskId, scanPhase, OpenTaskScanResultDO.TYPE_PORT_SCAN));
    }

    public List<Map<String, Object>> listLiveExportRowsBySub(String subId) {
        return toMaps(scanResultRepository.listBySubId(subId, OpenTaskScanResultDO.TYPE_LIVE_PROBE));
    }

    public List<Map<String, Object>> listPortExportRowsBySub(String subId) {
        return toMaps(scanResultRepository.listBySubId(subId, OpenTaskScanResultDO.TYPE_PORT_SCAN));
    }

    public List<Map<String, Object>> listVulnScanRowsBySub(String subId) {
        return toMaps(scanResultRepository.listBySubId(subId, OpenTaskScanResultDO.TYPE_VULN_SCAN));
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listVulnDatabaseListBySub(String subId) {
        List<OpenTaskScanResultDO> rows = scanResultRepository.listBySubId(
                subId, OpenTaskScanResultDO.TYPE_VULN_DATABASE);
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyList();
        }
        for (OpenTaskScanResultDO row : rows) {
            if (row == null || !OpenTaskScanResultDO.VULN_DATABASE_META_KEY.equals(row.getResultKey())) {
                continue;
            }
            if (!StringUtils.hasText(row.getPayloadJson())) {
                return Collections.emptyList();
            }
            JSONArray array = JSON.parseArray(row.getPayloadJson());
            if (array == null || array.isEmpty()) {
                return Collections.emptyList();
            }
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                Object item = array.get(i);
                if (item instanceof Map) {
                    list.add(new LinkedHashMap<>((Map<String, Object>) item));
                } else if (item instanceof JSONObject) {
                    list.add(new LinkedHashMap<>(((JSONObject) item)));
                }
            }
            return list;
        }
        return Collections.emptyList();
    }

    /**
     * 从落库记录组装 {@link TaskCenterSurveyBundle}，供实例 ingest / 验证合并使用（不再实时查 VTC）。
     */
    public TaskCenterSurveyBundle loadSurveyBundleBySub(String subId) {
        TaskCenterSurveyBundle bundle = new TaskCenterSurveyBundle();
        if (!StringUtils.hasText(subId)) {
            return bundle;
        }
        List<Map<String, Object>> liveRows = listLiveExportRowsBySub(subId);
        bundle.setSuccessIps(new HashSet<>(listSuccessIpsFromLiveRows(liveRows)));
        bundle.setFailIps(new HashSet<>(listFailIpsFromLiveRows(liveRows)));
        bundle.setPortScanRows(toVtcPortScanRows(listPortExportRowsBySub(subId)));
        bundle.setVulnScanResultList(listVulnScanRowsBySub(subId));
        bundle.setVulnDatabaseList(listVulnDatabaseListBySub(subId));
        bundle.setSuccessIpsQueryOk(true);
        bundle.setFailIpsQueryOk(true);
        bundle.setPortScanQueryOk(true);
        return bundle;
    }

    public boolean hasPersistedResults(String subId) {
        return !CollectionUtils.isEmpty(scanResultRepository.listBySubId(subId, null));
    }

    public List<String> listSuccessIpsFromLiveRows(List<Map<String, Object>> liveRows) {
        return listIpsByAlive(liveRows, true);
    }

    public List<String> listFailIpsFromLiveRows(List<Map<String, Object>> liveRows) {
        return listIpsByAlive(liveRows, false);
    }

    /**
     * 将 §5.6.5 端口行转回 VTC 工作台展示结构（按 IP 聚合 portInfoArray）。
     */
    public List<Map<String, Object>> toVtcPortScanRows(List<Map<String, Object>> exportRows) {
        if (CollectionUtils.isEmpty(exportRows)) {
            return Collections.emptyList();
        }
        Map<String, Map<String, Object>> byIp = new LinkedHashMap<>();
        for (Map<String, Object> row : exportRows) {
            if (row == null) {
                continue;
            }
            String ip = stringVal(row.get("address"));
            if (!StringUtils.hasText(ip)) {
                continue;
            }
            Map<String, Object> host = byIp.computeIfAbsent(ip, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("ip", k);
                m.put("portInfoArray", new ArrayList<Map<String, Object>>());
                return m;
            });
            String osName = stringVal(row.get("osName"));
            if (StringUtils.hasText(osName) && !host.containsKey("osName")) {
                host.put("osName", osName);
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ports = (List<Map<String, Object>>) host.get("portInfoArray");
            Map<String, Object> portInfo = new LinkedHashMap<>();
            portInfo.put("port", stringVal(row.get("port")));
            portInfo.put("protocol", stringVal(row.get("protocol")));
            portInfo.put("state", stringVal(row.get("state")));
            portInfo.put("service", stringVal(row.get("service")));
            portInfo.put("banner", stringVal(row.get("banner")));
            portInfo.put("version", stringVal(row.get("version")));
            portInfo.put("osName", stringVal(row.get("osName")));
            ports.add(portInfo);
        }
        return new ArrayList<>(byIp.values());
    }

    private List<String> listIpsByAlive(List<Map<String, Object>> liveRows, boolean alive) {
        if (CollectionUtils.isEmpty(liveRows)) {
            return Collections.emptyList();
        }
        Set<String> ips = new LinkedHashSet<>();
        for (Map<String, Object> row : liveRows) {
            if (row == null) {
                continue;
            }
            Object flag = row.get("alive");
            boolean isAlive = Boolean.TRUE.equals(flag);
            if (isAlive == alive) {
                String address = stringVal(row.get("address"));
                if (StringUtils.hasText(address)) {
                    ips.add(address);
                }
            }
        }
        return new ArrayList<>(ips);
    }

    private static String stringVal(Object value) {
        return value != null ? value.toString().trim() : null;
    }

    private List<Map<String, Object>> toMaps(List<OpenTaskScanResultDO> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (OpenTaskScanResultDO row : rows) {
            if (row == null || !StringUtils.hasText(row.getPayloadJson())) {
                continue;
            }
            JSONObject json = JSON.parseObject(row.getPayloadJson());
            if (json != null) {
                list.add(new LinkedHashMap<>(json));
            }
        }
        return list;
    }
}
