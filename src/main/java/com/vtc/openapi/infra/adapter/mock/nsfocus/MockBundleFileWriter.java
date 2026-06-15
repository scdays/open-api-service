package com.vtc.openapi.infra.adapter.mock.nsfocus;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Writes mock bundle files (instances.json + manifest.json).
 */
@Component
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "mock")
public class MockBundleFileWriter {

    public void writeBundle(Path outDir, String bundleId, OpenTaskDO task, NsfocusMockParseResult parseResult)
            throws IOException {
        Files.createDirectories(outDir);
        Map<String, Object> match = buildMatch(task);
        String description = String.format(
                "NSFocus %s scan (Java NsfocusMockXmlParser), task=%s, source=%s, count=%d, profile=%s",
                parseResult.getScanKind(),
                parseResult.getTaskId(),
                parseResult.getSourceXml(),
                parseResult.getInstances().size(),
                parseResult.getProfile());

        JSONObject instancesDoc = new JSONObject();
        instancesDoc.put("bundleId", bundleId);
        instancesDoc.put("description", description);
        instancesDoc.put("match", match);
        instancesDoc.put("instances", parseResult.getInstances());

        JSONObject manifest = new JSONObject();
        manifest.put("bundleId", bundleId);
        manifest.put("description", description);
        manifest.put("match", match);
        manifest.put("instanceCount", parseResult.getInstances().size());
        manifest.put("sourceXml", parseResult.getSourceXml());
        manifest.put("taskId", parseResult.getTaskId());
        manifest.put("taskName", parseResult.getTaskName());
        manifest.put("scanKind", parseResult.getScanKind());
        manifest.put("profile", parseResult.getProfile());
        manifest.put("parserRef", "NsfocusMockXmlParser");
        manifest.put("importedAt", LocalDate.now().toString());

        Files.write(outDir.resolve("instances.json"),
                JSON.toJSONString(instancesDoc).getBytes(StandardCharsets.UTF_8));
        Files.write(outDir.resolve("manifest.json"),
                JSON.toJSONString(manifest).getBytes(StandardCharsets.UTF_8));
    }

    private static Map<String, Object> buildMatch(OpenTaskDO task) {
        Map<String, Object> match = new HashMap<>();
        if (task != null) {
            if (StringUtils.hasText(task.getExtTaskId())) {
                match.put("extTaskIdPrefix", task.getExtTaskId());
            }
            if (StringUtils.hasText(task.getTaskName()) && task.getTaskName().length() <= 40) {
                match.put("taskNameContains", task.getTaskName());
            }
            if (task.getScanTemplateId() != null && task.getScanTemplateId() > 0) {
                match.put("scanTemplateId", task.getScanTemplateId());
            }
            if (task.getReportTemplateId() != null && task.getReportTemplateId() > 0) {
                match.put("reportTemplateIds", java.util.Collections.singletonList(task.getReportTemplateId()));
            }
            if (task.getVulnType() != null) {
                match.put("vulnTypes", java.util.Collections.singletonList(task.getVulnType()));
            }
        }
        return match;
    }
}
