package com.vtc.openapi.infra.adapter.mock.nsfocus;

import org.junit.Before;
import org.junit.Test;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NsfocusMockXmlParserTest {

    private NsfocusMockXmlParser parser;

    @Before
    public void setUp() {
        parser = new NsfocusMockXmlParser();
    }

    @Test
    public void parseVulProfileFromFixture() throws Exception {
        byte[] xml = readFixture("mock/nsfocus/report_by_vul.xml");
        NsfocusMockParseResult result = parser.parse(xml, "vul", 0, "report_by_vul.xml");

        assertEquals("58", result.getTaskId());
        assertEquals("vul", result.getProfile());
        assertEquals(NsfocusMockXmlParser.NSFOCUS_VENDOR, result.getVendor());
        assertTrue(result.getInstances().size() >= 2);

        boolean hasSsh = result.getInstances().stream()
                .anyMatch(i -> "77012".equals(i.getString("vulId").replace("VUL-", ""))
                        || i.getString("vulName").contains("OpenSSH"));
        assertTrue(hasSsh);
    }

    @Test
    public void templateValidatorMapsScanTemplate1001() {
        com.vtc.openapi.domain.task.model.entity.OpenTaskDO task =
                new com.vtc.openapi.domain.task.model.entity.OpenTaskDO();
        task.setScanTemplateId(1001);
        task.setVulnType(1);
        assertEquals("vul", MockXmlTemplateValidator.expectedProfileForTask(task));

        task.setVulnType(3);
        assertEquals("pwd", MockXmlTemplateValidator.expectedProfileForTask(task));
    }

    private static byte[] readFixture(String path) throws Exception {
        InputStream in = NsfocusMockXmlParserTest.class.getClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("missing fixture: " + path);
        }
        return StreamUtils.copyToByteArray(in);
    }
}
