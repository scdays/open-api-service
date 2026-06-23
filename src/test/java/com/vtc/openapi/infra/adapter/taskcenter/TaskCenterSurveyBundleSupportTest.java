package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.infra.feign.dto.taskcenter.TaskCenterSurveyBundle;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskCenterSurveyBundleSupportTest {

    @Test
    void emptyBundle_portOnlyIgnoresVulnRows() {
        TaskCenterSurveyBundle bundle = new TaskCenterSurveyBundle();
        bundle.setVulnScanResultList(Collections.singletonList(Collections.singletonMap("ip", "1.1.1.1")));
        bundle.setPortScanRows(Collections.emptyList());
        bundle.setPortScanQueryOk(true);

        assertTrue(TaskCenterSurveyBundleSupport.isEmptyBundle(bundle, ScanTemplateSurveyScope.portAndAlive()));
    }

    @Test
    void emptyBundle_aliveOnlyWithPortDataStillEmpty() {
        TaskCenterSurveyBundle bundle = new TaskCenterSurveyBundle();
        bundle.setPortScanRows(Collections.singletonList(Collections.singletonMap("ip", "1.1.1.1")));
        bundle.setSuccessIps(new HashSet<>(Arrays.asList("1.1.1.1")));
        bundle.setSuccessIpsQueryOk(true);
        bundle.setFailIpsQueryOk(true);

        assertFalse(TaskCenterSurveyBundleSupport.isEmptyBundle(bundle, ScanTemplateSurveyScope.aliveOnly()));
    }
}
