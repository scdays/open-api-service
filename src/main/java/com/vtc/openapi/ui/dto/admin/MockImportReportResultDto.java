package com.vtc.openapi.ui.dto.admin;

import lombok.Data;

/**
 * Result of manual mock report import.
 */
@Data
public class MockImportReportResultDto {

    private String taskId;

    private String bundleId;

    private int instanceCount;

    private String status;

    private boolean instancesIngested;

    private String ingestError;
}
