package com.vtc.openapi.infra.adapter.mock.nsfocus;

import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Validates parsed XML against open task scan template / vuln type expectations.
 */
@Component
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "mock")
public class MockXmlTemplateValidator {

    public void validate(OpenTaskDO task, NsfocusMockParseResult result) {
        if (result == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "parse result is empty");
        }
        if (!StringUtils.hasText(result.getVendor())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "XML vendor is missing");
        }
        if (!NsfocusMockXmlParser.NSFOCUS_VENDOR.equals(result.getVendor())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR,
                    "unsupported XML vendor: " + result.getVendor());
        }
        if (result.getInstances() == null || result.getInstances().isEmpty()) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR,
                    "no instances parsed from XML; check profile or report content");
        }
        String expectedProfile = expectedProfileForTask(task);
        if (expectedProfile != null && !expectedProfile.equals(result.getProfile())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR,
                    "XML profile '" + result.getProfile() + "' does not match scanTemplateId="
                            + task.getScanTemplateId() + " (expected '" + expectedProfile + "')");
        }
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
