package com.vtc.openapi.infra.adapter.mock.nsfocus;

import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MockXmlTemplateValidatorTest {

    @Test
    public void liveTemplateRejectsVulProfileInAutoMode() {
        OpenTaskDO task = new OpenTaskDO();
        task.setScanTemplateId(1002);

        try {
            validator("auto").validate(task, resultWithProfile("vul"));
            fail("expected profile mismatch");
        } catch (OpenApiException ex) {
            assertEquals(40001, ex.getCode());
        }
    }

    @Test
    public void manualIngestAcceptsVulOnPortTemplate() {
        OpenTaskDO task = new OpenTaskDO();
        task.setScanTemplateId(1003);

        validator("manual").validate(task, resultWithProfile("vul"));
    }

    @Test
    public void scanTemplate1001AcceptsCrossProfileReportsInAutoMode() {
        OpenTaskDO vulTask = new OpenTaskDO();
        vulTask.setScanTemplateId(1001);
        vulTask.setVulnType(1);

        MockXmlTemplateValidator validator = validator("auto");
        validator.validate(vulTask, resultWithProfile("pwd"));
        validator.validate(vulTask, resultWithProfile("vul"));
    }

    private static MockXmlTemplateValidator validator(String ingestMode) {
        return new MockXmlTemplateValidator("manual".equalsIgnoreCase(ingestMode));
    }

    private static NsfocusMockParseResult resultWithProfile(String profile) {
        NsfocusMockParseResult result = new NsfocusMockParseResult();
        result.setVendor(NsfocusMockXmlParser.NSFOCUS_VENDOR);
        result.setProfile(profile);
        result.getInstances().add(new JSONObject());
        return result;
    }
}
