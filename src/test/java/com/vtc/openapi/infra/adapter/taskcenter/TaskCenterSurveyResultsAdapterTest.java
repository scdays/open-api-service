package com.vtc.openapi.infra.adapter.taskcenter;

import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.infra.feign.dto.taskcenter.TaskCenterSurveyBundle;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskCenterSurveyResultsAdapterTest {

    private final TaskCenterSurveyResultsAdapter adapter = new TaskCenterSurveyResultsAdapter();

    @Test
    public void toVulnInstances_orgVulIdPrefersCveOverProductVulId() {
        Map<String, Object> row = new HashMap<>();
        row.put("classify", "Nessus");
        row.put("vulId", "111891");
        row.put("cve", "CVE-2016-10160");
        row.put("vulnName", "Test Vuln");
        row.put("level", "high");
        row.put("ip", "172.16.3.28");
        row.put("port", "27017");
        row.put("protocol", "tcp");
        row.put("service", "mongodb");

        TaskCenterSurveyBundle bundle = new TaskCenterSurveyBundle();
        bundle.setVulnScanResultList(Collections.singletonList(row));

        List<JSONObject> instances = adapter.toVulnInstances(bundle);
        Assert.assertEquals(1, instances.size());
        Assert.assertEquals("CVE-2016-10160", instances.get(0).getString("orgVulId"));
        Assert.assertEquals("CVE-2016-10160", instances.get(0).getString("cve"));
        Assert.assertEquals("111891", instances.get(0).getString("vulId"));
    }
}
