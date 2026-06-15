package com.vtc.openapi.infra.adapter;

import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.infra.adapter.dto.SvmpTaskCreateRequest;
import com.vtc.openapi.infra.adapter.dto.SvmpTaskProgressResult;
import com.vtc.openapi.infra.adapter.mock.MockEngineFixtureLoader;
import com.vtc.openapi.infra.adapter.mock.MockFixtureResolver;
import com.vtc.openapi.infra.config.OpenApiProperties;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SvmpEngineAdapterMockImplTest {

    private SvmpEngineAdapterMockImpl adapter;

    @Before
    public void setUp() {
        OpenApiProperties properties = new OpenApiProperties();
        properties.getEngine().setAdapterMode("mock");
        properties.getEngine().getMock().setTaskFinishDelaySeconds(0);
        MockEngineFixtureLoader loader = mock(MockEngineFixtureLoader.class);
        when(loader.listBundles()).thenReturn(java.util.Collections.emptyList());
        when(loader.resolveBundle(org.mockito.Matchers.anyString(), org.mockito.Matchers.anyString(),
                org.mockito.Matchers.any(), org.mockito.Matchers.any(), org.mockito.Matchers.any()))
                .thenReturn(null);
        MockFixtureResolver resolver = mock(MockFixtureResolver.class);
        when(resolver.resolve(org.mockito.Matchers.anyString(), org.mockito.Matchers.anyString(),
                org.mockito.Matchers.anyString())).thenReturn(null);
        IOpenTaskRepository openTaskRepository = mock(IOpenTaskRepository.class);
        adapter = new SvmpEngineAdapterMockImpl(properties, loader, resolver, openTaskRepository);
    }

    @Test
    public void createTaskThenProgressFinishedImmediately() {
        SvmpTaskCreateRequest req = new SvmpTaskCreateRequest();
        req.setTaskName("mock-task");
        String engineId = adapter.createTask(req).getEngineTaskId();
        assertTrue(engineId.startsWith("MOCK-ENG-"));

        SvmpTaskProgressResult progress = adapter.getTaskProgress(engineId);
        assertEquals("FINISHED", progress.getStatus());
        assertEquals(Integer.valueOf(100), progress.getProgress());
    }
}
