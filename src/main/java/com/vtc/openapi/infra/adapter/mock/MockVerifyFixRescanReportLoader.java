package com.vtc.openapi.infra.adapter.mock;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.infra.adapter.mock.nsfocus.NsfocusMockParseResult;
import com.vtc.openapi.infra.adapter.mock.nsfocus.NsfocusMockXmlParser;
import com.vtc.openapi.infra.config.OpenApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 读取修复核验复扫报告（job bundle 或关联任务 bundle）。
 */
@Component
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "mock")
public class MockVerifyFixRescanReportLoader {

    private static final Logger log = LoggerFactory.getLogger(MockVerifyFixRescanReportLoader.class);

    private final MockTaskDataPathResolver pathResolver;
    private final NsfocusMockXmlParser xmlParser;
    private final OpenApiProperties properties;
    private final IOpenTaskRepository openTaskRepository;

    public MockVerifyFixRescanReportLoader(MockTaskDataPathResolver pathResolver,
                                           NsfocusMockXmlParser xmlParser,
                                           OpenApiProperties properties,
                                           IOpenTaskRepository openTaskRepository) {
        this.pathResolver = pathResolver;
        this.xmlParser = xmlParser;
        this.properties = properties;
        this.openTaskRepository = openTaskRepository;
    }

    public void saveJobRescanXml(String jobId, byte[] xmlBytes) throws IOException {
        Path dir = pathResolver.verifyFixJobDir(jobId);
        Files.createDirectories(dir);
        Files.write(dir.resolve("rescan.xml"), xmlBytes);
    }

    public List<JSONObject> loadRescanInstances(String jobId, String fallbackTaskId) {
        List<JSONObject> fromJob = loadFromJobBundle(jobId);
        if (!CollectionUtils.isEmpty(fromJob)) {
            return fromJob;
        }
        if (StringUtils.hasText(fallbackTaskId)) {
            return loadFromTaskBundle(fallbackTaskId);
        }
        return Collections.emptyList();
    }

    public List<JSONObject> parseXml(byte[] xmlBytes, String taskId) {
        if (xmlBytes == null || xmlBytes.length == 0) {
            return Collections.emptyList();
        }
        String profile = properties.getEngine().getMock().getXmlImportProfile();
        NsfocusMockParseResult parsed = xmlParser.parse(xmlBytes, profile, 0, "verify-fix-rescan.xml");
        if (parsed == null || CollectionUtils.isEmpty(parsed.getInstances())) {
            return Collections.emptyList();
        }
        return new ArrayList<>(parsed.getInstances());
    }

    private List<JSONObject> loadFromJobBundle(String jobId) {
        Path xml = pathResolver.verifyFixJobDir(jobId).resolve("rescan.xml");
        if (!Files.isRegularFile(xml)) {
            return Collections.emptyList();
        }
        try {
            byte[] bytes = Files.readAllBytes(xml);
            return parseXml(bytes, null);
        } catch (IOException ex) {
            log.warn("read verify-fix rescan xml failed: jobId={}", jobId, ex);
            return Collections.emptyList();
        }
    }

    private List<JSONObject> loadFromTaskBundle(String taskId) {
        Path sourceXml = pathResolver.taskBundleDir(taskId).resolve("source.xml");
        if (!Files.isRegularFile(sourceXml)) {
            return Collections.emptyList();
        }
        try {
            return parseXml(Files.readAllBytes(sourceXml), taskId);
        } catch (IOException ex) {
            log.warn("read task source xml for verify-fix failed: taskId={}", taskId, ex);
            return Collections.emptyList();
        }
    }

    public List<JSONObject> loadFromTaskInstancesJson(String taskId) {
        Path instances = pathResolver.taskBundleDir(taskId).resolve("instances.json");
        if (!Files.isRegularFile(instances)) {
            return Collections.emptyList();
        }
        try {
            String json = new String(Files.readAllBytes(instances), StandardCharsets.UTF_8);
            JSONObject root = JSON.parseObject(json);
            if (root == null || root.getJSONArray("instances") == null) {
                return Collections.emptyList();
            }
            return root.getJSONArray("instances").toJavaList(JSONObject.class);
        } catch (IOException ex) {
            log.warn("read task instances.json failed: taskId={}", taskId, ex);
            return Collections.emptyList();
        }
    }

    public OpenTaskDO requireTask(String taskId) {
        OpenTaskDO task = openTaskRepository.findByTaskId(taskId);
        if (task == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "关联任务不存在: " + taskId);
        }
        return task;
    }
}
