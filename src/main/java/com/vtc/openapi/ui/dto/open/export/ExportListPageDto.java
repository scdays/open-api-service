package com.vtc.openapi.ui.dto.open.export;

import lombok.Data;

import java.util.List;

@Data
public class ExportListPageDto {

    private Integer page;
    private Integer size;
    private Long total;
    private List<ExportMetadataDto> items;
}
