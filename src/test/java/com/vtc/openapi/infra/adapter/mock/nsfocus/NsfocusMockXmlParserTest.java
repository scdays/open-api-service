package com.vtc.openapi.infra.adapter.mock.nsfocus;

import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NsfocusMockXmlParserTest {

    private NsfocusMockXmlParser parser;
    private MockXmlTemplateValidator templateValidator;

    @Before
    public void setUp() {
        parser = new NsfocusMockXmlParser();
        templateValidator = new MockXmlTemplateValidator(true);
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
    public void parsePwdProfileFromFixture() throws Exception {
        byte[] xml = readFixture("mock/nsfocus/report_by_pwd.xml");
        NsfocusMockParseResult result = parser.parse(xml, "auto", 0, "report_by_pwd.xml");

        assertEquals("pwd", result.getProfile());
        assertTrue(result.getInstances().size() >= 1);
        assertEquals("test", result.getInstances().get(0).getString("username"));
        assertEquals("test", result.getInstances().get(0).getString("password"));
    }

    @Test
    public void templateValidatorAllowsVulAndPwdForScanTemplate1001() {
        OpenTaskDO vulTask = taskWithTemplate(1001, 1);
        OpenTaskDO pwdTask = taskWithTemplate(1001, 3);

        NsfocusMockParseResult vulResult = new NsfocusMockParseResult();
        vulResult.setVendor(NsfocusMockXmlParser.NSFOCUS_VENDOR);
        vulResult.setProfile("vul");
        vulResult.getInstances().add(new com.alibaba.fastjson.JSONObject());

        NsfocusMockParseResult pwdResult = new NsfocusMockParseResult();
        pwdResult.setVendor(NsfocusMockXmlParser.NSFOCUS_VENDOR);
        pwdResult.setProfile("pwd");
        pwdResult.getInstances().add(new com.alibaba.fastjson.JSONObject());

        templateValidator.validate(vulTask, vulResult);
        templateValidator.validate(pwdTask, pwdResult);
        templateValidator.validate(vulTask, pwdResult);
        templateValidator.validate(pwdTask, vulResult);
    }

    @Test
    public void templateValidatorMapsScanTemplate1001() {
        OpenTaskDO task = taskWithTemplate(1001, 1);
        assertEquals("vul", MockXmlTemplateValidator.expectedProfileForTask(task));

        task.setVulnType(3);
        assertEquals("pwd", MockXmlTemplateValidator.expectedProfileForTask(task));
    }

    @Test
    public void parseAllMockDataStandardsXmlIfPresent() throws Exception {
        Path mockDataDir = Paths.get("../../../svmp/docs/standards/mock-data").toAbsolutePath().normalize();
        if (!Files.isDirectory(mockDataDir)) {
            return;
        }
        parseAndAssert(mockDataDir.resolve("report_by_vul.xml"), "vul", 2);
        parseAndAssert(mockDataDir.resolve("report_by_pwd.xml"), "pwd", 1);
        parseAndAssert(mockDataDir.resolve("漏洞扫描结果1166.xml"), "vul", 100);
        parseAndAssert(mockDataDir.resolve("弱口令扫描结果1053.xml"), "pwd", 8);
    }

    private void parseAndAssert(Path xmlPath, String expectedProfile, int minInstances) throws Exception {
        if (!Files.isRegularFile(xmlPath)) {
            return;
        }
        byte[] xml = Files.readAllBytes(xmlPath);
        NsfocusMockParseResult result = parser.parse(xml, "auto", 0, xmlPath.getFileName().toString());
        assertEquals(expectedProfile, result.getProfile());
        assertFalse(result.getInstances().isEmpty());
        assertTrue("expected >= " + minInstances + " for " + xmlPath.getFileName(),
                result.getInstances().size() >= minInstances);
    }

    private static OpenTaskDO taskWithTemplate(int scanTemplateId, int vulnType) {
        OpenTaskDO task = new OpenTaskDO();
        task.setScanTemplateId(scanTemplateId);
        task.setVulnType(vulnType);
        return task;
    }

    private static byte[] readFixture(String path) throws Exception {
        InputStream in = NsfocusMockXmlParserTest.class.getClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("missing fixture: " + path);
        }
        return StreamUtils.copyToByteArray(in);
    }
}
