package com.vtc.openapi.infra.adapter.mock;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.infra.adapter.mock.nsfocus.MockBundleFileWriter;
import com.vtc.openapi.infra.adapter.mock.nsfocus.MockXmlTemplateValidator;
import com.vtc.openapi.infra.adapter.mock.nsfocus.NsfocusMockParseResult;
import com.vtc.openapi.infra.adapter.mock.nsfocus.NsfocusMockXmlParser;
import com.vtc.openapi.infra.config.OpenApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Import NSFocus Aurora XML into mock task bundles (Java parser or Python fallback).
 */
@Component
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "mock")
public class MockReportImportRunner {

    private static final Logger log = LoggerFactory.getLogger(MockReportImportRunner.class);

    private final OpenApiProperties properties;
    private final MockTaskDataPathResolver pathResolver;
    private final NsfocusMockXmlParser xmlParser;
    private final MockXmlTemplateValidator templateValidator;
    private final MockBundleFileWriter bundleFileWriter;

    public MockReportImportRunner(OpenApiProperties properties,
                                  MockTaskDataPathResolver pathResolver,
                                  NsfocusMockXmlParser xmlParser,
                                  MockXmlTemplateValidator templateValidator,
                                  MockBundleFileWriter bundleFileWriter) {
        this.properties = properties;
        this.pathResolver = pathResolver;
        this.xmlParser = xmlParser;
        this.templateValidator = templateValidator;
        this.bundleFileWriter = bundleFileWriter;
    }

    public int importXmlReport(OpenTaskDO task, byte[] xmlBytes) throws IOException, InterruptedException {
        String taskId = requireTaskId(task);
        Path taskDir = pathResolver.taskBundleDir(taskId);
        Files.createDirectories(taskDir);
        Path xmlPath = taskDir.resolve("source.xml");
        Files.write(xmlPath, xmlBytes);
        importToDirectory(task, xmlBytes, xmlPath.getFileName().toString(), taskDir, 0);
        return countInstances(taskDir);
    }

    public MockXmlParseOutcome previewXmlReport(OpenTaskDO task, byte[] xmlBytes, int sampleSize)
            throws IOException, InterruptedException {
        String taskId = requireTaskId(task);
        if (properties.getEngine().getMock().isJavaXmlImportMode()) {
            NsfocusMockParseResult parsed = parseWithJava(task, xmlBytes, "preview.xml", 0);
            templateValidator.validate(task, parsed);
            MockXmlParseOutcome outcome = new MockXmlParseOutcome();
            outcome.setTotalCount(parsed.getInstances().size());
            int limit = Math.max(1, Math.min(sampleSize, 50));
            List<JSONObject> all = parsed.getInstances();
            outcome.setSamples(all.size() <= limit
                    ? new ArrayList<>(all)
                    : new ArrayList<>(all.subList(0, limit)));
            return outcome;
        }
        Path previewDir = pathResolver.taskBundleDir(taskId).resolve("_preview_tmp");
        deleteRecursively(previewDir);
        Files.createDirectories(previewDir);
        Path xmlPath = previewDir.resolve("source.xml");
        Files.write(xmlPath, xmlBytes);
        try {
            runPythonImport(task, xmlPath, previewDir, null);
            List<JSONObject> instances = readInstances(previewDir);
            MockXmlParseOutcome outcome = new MockXmlParseOutcome();
            outcome.setTotalCount(instances.size());
            int limit = Math.max(1, Math.min(sampleSize, 50));
            outcome.setSamples(instances.size() <= limit
                    ? new ArrayList<>(instances)
                    : new ArrayList<>(instances.subList(0, limit)));
            return outcome;
        } finally {
            deleteRecursively(previewDir);
        }
    }

    public BundleFileStatus readBundleFiles(String taskId) throws IOException {
        Path taskDir = pathResolver.taskBundleDir(taskId);
        BundleFileStatus status = new BundleFileStatus();
        status.setTaskBundleDir(taskDir.toAbsolutePath().toString());
        Path sourceXml = taskDir.resolve("source.xml");
        status.setHasSourceXml(Files.isRegularFile(sourceXml));
        Path instances = taskDir.resolve("instances.json");
        if (Files.isRegularFile(instances)) {
            status.setBundleInstanceCount(countInstances(taskDir));
            JSONObject root = JSON.parseObject(new String(Files.readAllBytes(instances), StandardCharsets.UTF_8));
            if (root != null) {
                status.setBundleId(root.getString("bundleId"));
            }
        }
        Path manifest = taskDir.resolve("manifest.json");
        if (Files.isRegularFile(manifest)) {
            JSONObject manifestJson = JSON.parseObject(new String(Files.readAllBytes(manifest), StandardCharsets.UTF_8));
            if (manifestJson != null) {
                status.setImportedAt(manifestJson.getString("importedAt"));
                if (!StringUtils.hasText(status.getBundleId())) {
                    status.setBundleId(manifestJson.getString("bundleId"));
                }
            }
        }
        return status;
    }

    private void importToDirectory(OpenTaskDO task, byte[] xmlBytes, String sourceName, Path outDir, int limit)
            throws IOException, InterruptedException {
        if (properties.getEngine().getMock().isJavaXmlImportMode()) {
            NsfocusMockParseResult parsed = parseWithJava(task, xmlBytes, sourceName, limit);
            templateValidator.validate(task, parsed);
            String bundleId = "task-" + task.getTaskId();
            bundleFileWriter.writeBundle(outDir, bundleId, task, parsed);
            return;
        }
        Path xmlPath = outDir.resolve("source.xml");
        if (!Files.isRegularFile(xmlPath)) {
            Files.write(xmlPath, xmlBytes);
        }
        runPythonImport(task, xmlPath, outDir, limit > 0 ? limit : null);
    }

    private NsfocusMockParseResult parseWithJava(OpenTaskDO task, byte[] xmlBytes, String sourceName, int limit) {
        String profile = properties.getEngine().getMock().getXmlImportProfile();
        return xmlParser.parse(xmlBytes, profile, limit, sourceName);
    }

    private void runPythonImport(OpenTaskDO task, Path xmlPath, Path outDir, Integer limit)
            throws IOException, InterruptedException {
        Path script = resolveScriptPath();
        if (!Files.isRegularFile(script)) {
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED,
                    "import script not found: " + script.toAbsolutePath());
        }

        String bundleId = "task-" + task.getTaskId();
        List<String> command = new ArrayList<>();
        command.add(properties.getEngine().getMock().getPythonCommand());
        command.add(script.toAbsolutePath().toString());
        command.add("--xml");
        command.add(xmlPath.toAbsolutePath().toString());
        command.add("--bundle-id");
        command.add(bundleId);
        command.add("--out");
        command.add(outDir.toAbsolutePath().toString());
        if (limit != null && limit > 0) {
            command.add("--limit");
            command.add(String.valueOf(limit));
        }
        String profile = properties.getEngine().getMock().getXmlImportProfile();
        if (StringUtils.hasText(profile)) {
            command.add("--profile");
            command.add(profile);
        }
        if (task.getScanTemplateId() != null && task.getScanTemplateId() > 0) {
            command.add("--scan-template-id");
            command.add(String.valueOf(task.getScanTemplateId()));
        }
        if (task.getReportTemplateId() != null && task.getReportTemplateId() > 0) {
            command.add("--report-template-ids");
            command.add(String.valueOf(task.getReportTemplateId()));
        }
        if (task.getVulnType() != null) {
            command.add("--vuln-types");
            command.add(String.valueOf(task.getVulnType()));
        }
        if (StringUtils.hasText(task.getExtTaskId())) {
            command.add("--match-ext-prefix");
            command.add(task.getExtTaskId());
        }
        if (StringUtils.hasText(task.getTaskName())) {
            command.add("--match-task-name");
            command.add(task.getTaskName());
        }

        log.info("Running mock XML import (python): taskId={} script={} out={}", task.getTaskId(), script, outDir);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = readProcessOutput(process);
        int exit = process.waitFor();
        if (exit != 0) {
            log.warn("Mock XML import failed exit={} output={}", exit, output);
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED,
                    "XML import failed (exit " + exit + "): " + truncate(output, 500));
        }
        log.info("Mock XML import ok taskId={} output={}", task.getTaskId(), truncate(output, 200));
    }

    private static String requireTaskId(OpenTaskDO task) {
        if (task == null || !StringUtils.hasText(task.getTaskId())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "taskId is required");
        }
        return task.getTaskId();
    }

    private Path resolveScriptPath() {
        String configured = properties.getEngine().getMock().getImportScriptPath();
        if (StringUtils.hasText(configured)) {
            Path p = Paths.get(configured.trim());
            if (p.isAbsolute() && Files.isRegularFile(p)) {
                return p;
            }
            Path fromUserDir = Paths.get(System.getProperty("user.dir")).resolve(configured.trim());
            if (Files.isRegularFile(fromUserDir)) {
                return fromUserDir;
            }
            return p;
        }
        Path fallback = Paths.get(System.getProperty("user.dir"))
                .resolve("svmp/docs/internal/scripts/import-nsfocus-xml-to-mock-bundle.py");
        if (Files.isRegularFile(fallback)) {
            return fallback;
        }
        return fallback;
    }

    private static List<JSONObject> readInstances(Path taskDir) throws IOException {
        Path instances = taskDir.resolve("instances.json");
        if (!Files.isRegularFile(instances)) {
            return Collections.emptyList();
        }
        String json = new String(Files.readAllBytes(instances), StandardCharsets.UTF_8);
        JSONObject root = JSON.parseObject(json);
        if (root == null || root.getJSONArray("instances") == null) {
            return Collections.emptyList();
        }
        return root.getJSONArray("instances").toJavaList(JSONObject.class);
    }

    private static int countInstances(Path taskDir) throws IOException {
        return readInstances(taskDir).size();
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> paths = walk.sorted((a, b) -> b.compareTo(a)).collect(Collectors.toList());
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static String readProcessOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    public static class MockXmlParseOutcome {
        private int totalCount;
        private List<JSONObject> samples = new ArrayList<>();

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public List<JSONObject> getSamples() {
            return samples;
        }

        public void setSamples(List<JSONObject> samples) {
            this.samples = samples != null ? samples : new ArrayList<>();
        }
    }

    public static class BundleFileStatus {
        private boolean hasSourceXml;
        private int bundleInstanceCount;
        private String bundleId;
        private String importedAt;
        private String taskBundleDir;

        public boolean isHasSourceXml() {
            return hasSourceXml;
        }

        public void setHasSourceXml(boolean hasSourceXml) {
            this.hasSourceXml = hasSourceXml;
        }

        public int getBundleInstanceCount() {
            return bundleInstanceCount;
        }

        public void setBundleInstanceCount(int bundleInstanceCount) {
            this.bundleInstanceCount = bundleInstanceCount;
        }

        public String getBundleId() {
            return bundleId;
        }

        public void setBundleId(String bundleId) {
            this.bundleId = bundleId;
        }

        public String getImportedAt() {
            return importedAt;
        }

        public void setImportedAt(String importedAt) {
            this.importedAt = importedAt;
        }

        public String getTaskBundleDir() {
            return taskBundleDir;
        }

        public void setTaskBundleDir(String taskBundleDir) {
            this.taskBundleDir = taskBundleDir;
        }
    }
}
