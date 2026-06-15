package com.vtc.openapi.ui.dto.admin;

import lombok.Data;

/**
 * Task bundle + ingest status for mock manual workflow UI.
 */
@Data
public class MockBundleStatusDto {

    private String taskId;

    private String partnerId;

    private String status;

    private String ingestMode;

    private Boolean instancesIngested;

    private String ingestError;

    private long persistedInstanceCount;

    private boolean hasSourceXml;

    private int bundleInstanceCount;

    private String bundleId;

    private String importedAt;

    private String taskBundleDir;
}
