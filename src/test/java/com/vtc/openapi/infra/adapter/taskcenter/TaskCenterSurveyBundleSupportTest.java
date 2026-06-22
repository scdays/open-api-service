package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.infra.feign.dto.taskcenter.TaskCenterSurveyBundle;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TaskCenterSurveyBundleSupportTest {

    @Test
    public void isLikelyVtcLagWhenAllQueriesOkButEmpty() {
        TaskCenterSurveyBundle bundle = new TaskCenterSurveyBundle();
        bundle.setSuccessIpsQueryOk(true);
        bundle.setFailIpsQueryOk(true);
        bundle.setPortScanQueryOk(true);
        assertTrue(TaskCenterSurveyBundleSupport.isLikelyVtcLag(bundle));
    }

    @Test
    public void notLagWhenHasVulnRows() {
        TaskCenterSurveyBundle bundle = new TaskCenterSurveyBundle();
        bundle.getVulnScanResultList().add(Collections.singletonMap("vulId", "1"));
        assertFalse(TaskCenterSurveyBundleSupport.isLikelyVtcLag(bundle));
    }

    @Test
    public void notLagWhenQueryFailed() {
        TaskCenterSurveyBundle bundle = new TaskCenterSurveyBundle();
        bundle.setPortScanQueryOk(false);
        assertFalse(TaskCenterSurveyBundleSupport.isLikelyVtcLag(bundle));
    }
}
