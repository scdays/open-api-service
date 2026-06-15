package com.vtc.openapi.infra.adapter.task;

import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.task.model.result.ParsedScanTaskFileResult;
import org.junit.Assert;
import org.junit.Test;

public class ScanTaskXmlParserTest {

    private final ScanTaskXmlParser parser = new ScanTaskXmlParser();

    @Test
    public void parse_validTemplateModeA() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<scanTask>"
                + "<server>"
                + "<taskName>2026Q2-WEB</taskName>"
                + "<priority>HIGH</priority>"
                + "<targets>https://www.example.com/</targets>"
                + "</server>"
                + "<targets/>"
                + "<scanTemplateId>1001</scanTemplateId>"
                + "<reportTemplateId>2001</reportTemplateId>"
                + "</scanTask>";

        ParsedScanTaskFileResult parsed = parser.parse(xml);
        Assert.assertEquals("2026Q2-WEB", parsed.getTaskName());
        Assert.assertEquals("https://www.example.com/", parsed.getTargets().getHosts());
        Assert.assertEquals(Integer.valueOf(1001), parsed.getScanTemplateId());
        Assert.assertEquals(Integer.valueOf(2001), parsed.getReportTemplateId());
    }

    @Test
    public void parse_missingServer_throws40001() {
        try {
            parser.parse("<scanTask><targets/></scanTask>");
            Assert.fail("expected OpenApiException");
        } catch (OpenApiException ex) {
            Assert.assertEquals(40001, ex.getCode());
            Assert.assertTrue(ex.getMessage().contains("server"));
        }
    }

    @Test
    public void parse_invalidRoot_throws40001() {
        try {
            parser.parse("<task><server><taskName>x</taskName><targets>1.1.1.1</targets></server><targets/></task>");
            Assert.fail("expected OpenApiException");
        } catch (OpenApiException ex) {
            Assert.assertEquals(40001, ex.getCode());
        }
    }
}
