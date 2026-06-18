package com.vtc.openapi.infra.adapter.taskcenter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TaskCenterVerifyStatusResolverTest {

    private TaskCenterVerifyStatusResolver resolver;

    @Before
    public void setUp() {
        resolver = new TaskCenterVerifyStatusResolver(new TaskCenterVerifyMergeService());
    }

    @Test
    public void union_bothScanners_hit2() {
        Assert.assertEquals(2, resolver.resolveVerifyStat(2, 2, TaskCenterVerifyStatusResolver.STRATEGY_UNION));
    }

    @Test
    public void union_singleScanner_hit1() {
        Assert.assertEquals(1, resolver.resolveVerifyStat(1, 2, TaskCenterVerifyStatusResolver.STRATEGY_UNION));
    }

    @Test
    public void union_noHit_falsePositive3() {
        Assert.assertEquals(3, resolver.resolveVerifyStat(0, 2, TaskCenterVerifyStatusResolver.STRATEGY_UNION));
    }

    @Test
    public void intersect_bothScanners_hit2() {
        Assert.assertEquals(2, resolver.resolveVerifyStat(2, 2, TaskCenterVerifyStatusResolver.STRATEGY_INTERSECT));
    }

    @Test
    public void intersect_singleScanner_falsePositive3() {
        Assert.assertEquals(3, resolver.resolveVerifyStat(1, 2, TaskCenterVerifyStatusResolver.STRATEGY_INTERSECT));
    }

    @Test
    public void intersect_noHit_falsePositive3() {
        Assert.assertEquals(3, resolver.resolveVerifyStat(0, 2, TaskCenterVerifyStatusResolver.STRATEGY_INTERSECT));
    }
}
