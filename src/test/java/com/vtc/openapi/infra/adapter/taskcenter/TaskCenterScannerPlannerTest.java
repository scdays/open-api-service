package com.vtc.openapi.infra.adapter.taskcenter;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class TaskCenterScannerPlannerTest {

    @Test
    public void crossScanOnlyFor1001() {
        Assert.assertTrue(TaskCenterScannerPlanner.isCrossScan(1001));
        Assert.assertFalse(TaskCenterScannerPlanner.isCrossScan(1002));
        Assert.assertFalse(TaskCenterScannerPlanner.isCrossScan(1003));
    }

    @Test
    public void resolveScannerTypes() {
        Assert.assertEquals(Arrays.asList("1", "7"),
                TaskCenterScannerPlanner.resolveScannerTypes(1001));
        Assert.assertEquals(Collections.singletonList("1"),
                TaskCenterScannerPlanner.resolveScannerTypes(1002));
        Assert.assertEquals(Collections.singletonList("1"),
                TaskCenterScannerPlanner.resolveScannerTypes(1003));
    }

    @Test
    public void unionStrategyForCrossScan() {
        Assert.assertEquals("UNION", TaskCenterScannerPlanner.resolveVerifyMergeStrategy(1001));
        Assert.assertEquals("INTERSECT", TaskCenterScannerPlanner.resolveVerifyMergeStrategy(1002));
    }
}
