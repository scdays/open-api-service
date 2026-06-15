package com.vtc.openapi.domain.export.model.result;

import lombok.Data;

import java.util.List;

@Data
public class ExportListResult {

    private int page;
    private int size;
    private long total;
    private List<ExportMetadataResult> items;
}
