package com.vtc.openapi.ui.dto.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * XML parse preview without persisting or finishing task.
 */
@Data
public class MockImportPreviewResultDto {

    private String taskId;

    private int totalCount;

    private int sampleSize;

    private List<MockImportPreviewItemDto> samples = new ArrayList<>();
}
