package com.vtc.openapi.ui.dto.admin;

import lombok.Data;

/**
 * Parsed instance row for import preview table.
 */
@Data
public class MockImportPreviewItemDto {

    private String vulName;

    private String vulNetAddr;

    private Integer vulPort;

    private Integer vulLevel;

    private Integer vulInfoStat;

    private String orgVulId;
}
