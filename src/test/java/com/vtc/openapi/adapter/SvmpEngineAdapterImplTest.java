package com.vtc.openapi.adapter;

import com.vtc.openapi.adapter.dto.SvmpTaskCreateRequest;
import com.vtc.openapi.adapter.dto.SvmpTaskCreateResult;
import com.vtc.openapi.adapter.dto.SvmpTaskProgressResult;
import com.vtc.openapi.common.OpenApiException;
import com.vtc.openapi.infra.config.OpenApiProperties;
import com.vtc.openapi.infra.feign.IVulPassScanTaskFeign;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SvmpEngineAdapterImplTest {

    private IVulPassScanTaskFeign vulPassFeign;
    private OpenApiProperties properties;
    private SvmpEngineAdapterImpl adapter;

    @Before
    public void setUp() {
        vulPassFeign = mock(IVulPassScanTaskFeign.class);
        properties = new OpenApiProperties();
        properties.getSvmp().getDispatch().setOrderId("1-31-0000000000000000001");
        adapter = new SvmpEngineAdapterImpl(vulPassFeign, properties);
    }

    @Test
    public void createTask_mapsDispatchAndResolvesEngineTaskId() {
        when(vulPassFeign.dispatch(any())).thenReturn("{\"success\":true,\"message\":\"操作成功\"}");
        when(vulPassFeign.pageTasks(1, 1, null, "1-31-0000000000000000001"))
                .thenReturn("{\"records\":[{\"id\":9527,\"tskProgress\":1,\"tskStat\":0}]}");

        SvmpTaskCreateRequest request = new SvmpTaskCreateRequest();
        request.setTaskName("open-api-test");
        request.setTargets(Arrays.asList("10.0.0.1", "10.0.0.2"));

        SvmpTaskCreateResult result = adapter.createTask(request);

        assertEquals("9527", result.getEngineTaskId());
    }

    @Test
    public void getTaskProgress_mapsTskProgressAndStat() {
        when(vulPassFeign.pageTasks(1, 1, 9527L, null))
                .thenReturn("{\"records\":[{\"id\":9527,\"tskProgress\":80,\"tskStat\":3}]}");

        SvmpTaskProgressResult progress = adapter.getTaskProgress("9527");

        assertEquals(80, progress.getProgress().intValue());
        assertEquals("RUNNING", progress.getStatus());
    }

    @Test
    public void createTask_withoutOrderId_throwsEngineFailed() {
        properties.getSvmp().getDispatch().setOrderId("");
        SvmpTaskCreateRequest request = new SvmpTaskCreateRequest();
        request.setTaskName("x");
        request.setTargets(Collections.singletonList("10.0.0.1"));
        try {
            adapter.createTask(request);
            fail("expected OpenApiException");
        } catch (OpenApiException ex) {
            assertNotNull(ex.getMessage());
        }
    }
}
