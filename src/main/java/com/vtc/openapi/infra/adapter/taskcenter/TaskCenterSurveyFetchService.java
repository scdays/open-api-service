package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.infra.feign.IVulnTaskCenterScanClient;
import com.vtc.openapi.infra.feign.dto.taskcenter.TaskCenterSurveyBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class TaskCenterSurveyFetchService {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterSurveyFetchService.class);

    private final IVulnTaskCenterScanClient scanClient;

    public TaskCenterSurveyFetchService(IVulnTaskCenterScanClient scanClient) {
        this.scanClient = scanClient;
    }

    public TaskCenterSurveyBundle fetchAll(String surveyId) {
        TaskCenterSurveyBundle bundle = new TaskCenterSurveyBundle();
        bundle.setSurveyId(surveyId);
        if (!StringUtils.hasText(surveyId)) {
            return bundle;
        }
        bundle.setVulnScanResultList(fetchAllVulnRows(surveyId));
        bundle.setVulnDatabaseList(fetchVulnDatabaseList(surveyId));
        fetchAliveIpsResilient(surveyId, bundle);
        fetchPortScanResilient(surveyId, bundle);
        return bundle;
    }

    /**
     * VTC 存活主机需分别查询成功/失败列表，两次查询独立容错。
     */
    private void fetchAliveIpsResilient(String surveyId, TaskCenterSurveyBundle bundle) {
        boolean successOk = false;
        boolean failOk = false;
        try {
            Set<String> success = safeSet(scanClient.querySuccessIps(surveyId));
            bundle.setSuccessIps(success);
            successOk = true;
        } catch (Exception ex) {
            log.warn("querySuccessIps failed surveyId={}: {}", surveyId, ex.getMessage());
            bundle.setSuccessIps(new HashSet<>());
        }
        try {
            Set<String> fail = safeSet(scanClient.queryFailIps(surveyId));
            bundle.setFailIps(fail);
            failOk = true;
        } catch (Exception ex) {
            log.warn("queryFailIps failed surveyId={}: {}", surveyId, ex.getMessage());
            bundle.setFailIps(new HashSet<>());
        }
        bundle.setSuccessIpsQueryOk(successOk);
        bundle.setFailIpsQueryOk(failOk);
    }

    private void fetchPortScanResilient(String surveyId, TaskCenterSurveyBundle bundle) {
        try {
            List<Map<String, Object>> ports = scanClient.queryScanPorts(surveyId);
            bundle.setPortScanRows(ports != null ? ports : Collections.emptyList());
            bundle.setPortScanQueryOk(true);
        } catch (Exception ex) {
            log.warn("queryScanPorts failed surveyId={}: {}", surveyId, ex.getMessage());
            bundle.setPortScanRows(Collections.emptyList());
            bundle.setPortScanQueryOk(false);
        }
    }

    private List<Map<String, Object>> fetchAllVulnRows(String surveyId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        long current = 1L;
        long pages = 1L;
        while (current <= pages) {
            Map<String, Object> page;
            try {
                page = scanClient.queryVulnScanResult(surveyId, String.valueOf(current));
            } catch (Exception ex) {
                log.warn("queryVulnScanResult failed surveyId={} page={}: {}", surveyId, current, ex.getMessage());
                break;
            }
            if (page == null || page.isEmpty()) {
                break;
            }
            pages = toLong(page.get("pages"), 1L);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> part = (List<Map<String, Object>>) page.get("vulnScanResultList");
            if (!CollectionUtils.isEmpty(part)) {
                rows.addAll(part);
            }
            current++;
        }
        return rows;
    }

    private List<Map<String, Object>> fetchVulnDatabaseList(String surveyId) {
        try {
            Map<String, Object> firstPage = scanClient.queryVulnScanResult(surveyId, "1");
            if (firstPage == null) {
                return Collections.emptyList();
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dbList = (List<Map<String, Object>>) firstPage.get("vulnDatabaseList");
            return dbList != null ? dbList : Collections.emptyList();
        } catch (Exception ex) {
            log.warn("fetchVulnDatabaseList failed surveyId={}: {}", surveyId, ex.getMessage());
            return Collections.emptyList();
        }
    }

    private static Set<String> safeSet(Set<String> ips) {
        return ips != null ? ips : new HashSet<>();
    }

    private static long toLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
