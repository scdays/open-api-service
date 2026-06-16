package com.vtc.openapi.infra.adapter.mock.nsfocus;

import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.infra.config.OpenApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Validates parsed XML against open task scan template / vuln type expectations.
 */
@Component
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "mock")
public class MockXmlTemplateValidator {

    private static final Logger log = LoggerFactory.getLogger(MockXmlTemplateValidator.class);

    /** Resolved once at startup to avoid NPE when reading ingest-mode during validate. */
    private final boolean manualIngestMode;

    @Autowired
    public MockXmlTemplateValidator(OpenApiProperties properties) {
        this.manualIngestMode = resolveManualIngestMode(properties);
    }

    /** Unit tests and non-Spring callers. */
    public MockXmlTemplateValidator(boolean manualIngestMode) {
        this.manualIngestMode = manualIngestMode;
    }

    public void validate(OpenTaskDO task, NsfocusMockParseResult result) {
        if (task == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "task is required");
        }
        if (result == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "parse result is empty");
        }
        if (!StringUtils.hasText(result.getVendor())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "XML vendor is missing");
        }
        if (!NsfocusMockXmlParser.isNsfocusVendor(result.getVendor())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR,
                    "unsupported XML vendor: " + result.getVendor());
        }
        if (result.getInstances() == null || result.getInstances().isEmpty()) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR,
                    "no instances parsed from XML; check profile or report content");
        }
        if (manualIngestMode) {
            String expectedProfile = expectedProfileForTask(task);
            if (expectedProfile != null && !expectedProfile.equals(result.getProfile())) {
                log.info("manual ingest: XML profile '{}' differs from scanTemplateId={} hint '{}'; accepted",
                        result.getProfile(), task.getScanTemplateId(), expectedProfile);
            }
            return;
        }
        String expectedProfile = expectedProfileForTask(task);
        if (expectedProfile != null && !isProfileCompatible(task, expectedProfile, result.getProfile())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR,
                    "XML profile '" + result.getProfile() + "' does not match scanTemplateId="
                            + task.getScanTemplateId() + " (expected '" + expectedProfile + "')");
        }
    }

    private static boolean resolveManualIngestMode(OpenApiProperties properties) {
        if (properties == null) {
            log.warn("OpenApiProperties not injected; skip XML profile strict validation (manual ingest)");
            return true;
        }
        try {
            return properties.getEngine().getMock().isManualIngestMode();
        } catch (Exception ex) {
            log.warn("Failed to read ingest-mode from OpenApiProperties: {}", ex.getMessage());
            return true;
        }
    }

    /**
     * Auto ingest may upload the scanner's native report (vul or pwd) for template 1001.
     */
    private static boolean isProfileCompatible(OpenTaskDO task, String expected, String actual) {
        if (!StringUtils.hasText(expected) || !StringUtils.hasText(actual)) {
            return true;
        }
        if (expected.equals(actual)) {
            return true;
        }
        if (task.getScanTemplateId() != null && task.getScanTemplateId() == 1001) {
            return ("vul".equals(expected) || "pwd".equals(expected))
                    && ("vul".equals(actual) || "pwd".equals(actual));
        }
        return false;
    }

    /**
     * Map mock scan templates to XML parse profiles (aligned with classpath bundles).
     */
    public static String expectedProfileForTask(OpenTaskDO task) {
        if (task == null || task.getScanTemplateId() == null) {
            return null;
        }
        switch (task.getScanTemplateId()) {
            case 1002:
                return "live";
            case 1003:
                return "port";
            case 1001:
                if (task.getVulnType() != null && task.getVulnType() == 3) {
                    return "pwd";
                }
                return "vul";
            default:
                return null;
        }
    }
}
