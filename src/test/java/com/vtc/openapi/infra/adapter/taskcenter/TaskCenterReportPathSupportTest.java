package com.vtc.openapi.infra.adapter.taskcenter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TaskCenterReportPathSupportTest {

    @Test
    public void parseUnixPath() {
        TaskCenterReportPathSupport.ParsedReportPath parsed =
                TaskCenterReportPathSupport.parse("/vulnerability/rawResult/survey-1/report.xml");
        assertNotNull(parsed);
        assertEquals("/vulnerability/rawResult/survey-1", parsed.getRemoteDir());
        assertEquals("report.xml", parsed.getFileName());
    }

    @Test
    public void parseBlankReturnsNull() {
        assertNull(TaskCenterReportPathSupport.parse(" "));
    }

    @Test
    public void buildArchiveFileNameUsesSubId() {
        String name = TaskCenterReportPathSupport.buildArchiveFileName("SUB-001", "lm.xml");
        assertEquals("scan-report-SUB-001-lm.xml", name);
    }
}
