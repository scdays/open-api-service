package com.vtc.openapi.infra.adapter.taskcenter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScanTemplateSurveyScopeTest {

    @Test
    void template1002_aliveOnly() {
        ScanTemplateSurveyScope scope = ScanTemplateSurveyScope.fromScanTemplateId(1002);
        assertTrue(scope.needsAliveProbe());
        assertFalse(scope.needsPortScan());
        assertFalse(scope.needsVulnScan());
    }

    @Test
    void template1003_portAndAlive() {
        ScanTemplateSurveyScope scope = ScanTemplateSurveyScope.fromScanTemplateId(1003);
        assertTrue(scope.needsAliveProbe());
        assertTrue(scope.needsPortScan());
        assertFalse(scope.needsVulnScan());
    }

    @Test
    void template1001_fullSurvey() {
        ScanTemplateSurveyScope scope = ScanTemplateSurveyScope.fromScanTemplateId(1001);
        assertTrue(scope.needsAliveProbe());
        assertTrue(scope.needsPortScan());
        assertTrue(scope.needsVulnScan());
    }

    @Test
    void centerTaskTypeMapsLikeTemplate() {
        assertTrue(ScanTemplateSurveyScope.fromCenterTaskType("alive").needsAliveProbe());
        assertTrue(ScanTemplateSurveyScope.fromCenterTaskType("port").needsAliveProbe());
        assertFalse(ScanTemplateSurveyScope.fromCenterTaskType("port").needsVulnScan());
        assertTrue(ScanTemplateSurveyScope.fromCenterTaskType("vuln").needsVulnScan());
    }
}
