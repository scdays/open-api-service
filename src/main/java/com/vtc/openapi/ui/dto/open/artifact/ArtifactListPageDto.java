package com.vtc.openapi.ui.dto.open.artifact;

import lombok.Data;

import java.util.List;

@Data
public class ArtifactListPageDto {

    private Integer page;
    private Integer size;
    private Long total;
    private List<ArtifactMetadataDto> items;
}
