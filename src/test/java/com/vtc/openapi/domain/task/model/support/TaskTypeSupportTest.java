package com.vtc.openapi.domain.task.model.support;

import com.vtc.openapi.domain.open.OpenApiException;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class TaskTypeSupportTest {

    @Test
    public void requireValidType_accepts123() {
        TaskTypeSupport.requireValidType(1);
        TaskTypeSupport.requireValidType(2);
        TaskTypeSupport.requireValidType(3);
    }

    @Test
    public void requireValidType_rejectsInvalid() {
        try {
            TaskTypeSupport.requireValidType(4);
            Assert.fail("expected OpenApiException");
        } catch (OpenApiException ex) {
            Assert.assertEquals(40004, ex.getCode());
        }
    }

    @Test
    public void splitHosts_supportsCommaAndSemicolon() {
        List<String> hosts = TaskTypeSupport.splitHosts("10.0.0.1,10.0.0.2;10.0.0.3");
        Assert.assertEquals(3, hosts.size());
        Assert.assertEquals("10.0.0.1", hosts.get(0));
    }

    @Test
    public void normalizeProgressStatus_mapsAcceptedToPending() {
        Assert.assertEquals("PENDING", TaskTypeSupport.normalizeProgressStatus("ACCEPTED"));
        Assert.assertEquals("RUNNING", TaskTypeSupport.normalizeProgressStatus("RUNNING"));
    }
}
