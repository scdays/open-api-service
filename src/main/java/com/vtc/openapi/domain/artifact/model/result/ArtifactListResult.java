package com.vtc.openapi.domain.artifact.model.result;

import lombok.Data;

import java.util.List;

@Data
public class ArtifactListResult {

    private int page;
    private int size;
    private long total;
    private List<ArtifactMetadataResult> items;
}
