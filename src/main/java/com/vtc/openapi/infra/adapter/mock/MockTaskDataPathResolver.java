package com.vtc.openapi.infra.adapter.mock;

import com.vtc.openapi.infra.config.OpenApiProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolve writable mock data root and per-task bundle directories.
 */
@Component
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "mock")
public class MockTaskDataPathResolver {

    private final OpenApiProperties properties;

    public MockTaskDataPathResolver(OpenApiProperties properties) {
        this.properties = properties;
    }

    /**
     * Writable root: file: data-dir, or {@code ${java.io.tmpdir}/open-api-mock} for classpath.
     */
    public Path writableRoot() {
        String dataDir = properties.getEngine().getMock().getDataDir();
        if (StringUtils.hasText(dataDir) && dataDir.startsWith("file:")) {
            return Paths.get(URI.create(dataDir));
        }
        return Paths.get(System.getProperty("java.io.tmpdir"), "open-api-mock");
    }

    public Path taskBundleDir(String taskId) {
        return writableRoot().resolve("tasks").resolve(taskId);
    }

    public Path verifyFixJobDir(String jobId) {
        return writableRoot().resolve("verify-fix-jobs").resolve(jobId);
    }

    public String dataDirPatternBase() {
        String dataDir = properties.getEngine().getMock().getDataDir();
        if (!StringUtils.hasText(dataDir)) {
            return "classpath:mock/engine";
        }
        return dataDir.endsWith("/") ? dataDir : dataDir + "/";
    }
}
