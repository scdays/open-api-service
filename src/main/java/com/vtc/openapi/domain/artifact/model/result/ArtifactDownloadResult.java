package com.vtc.openapi.domain.artifact.model.result;

import lombok.Data;

@Data
public class ArtifactDownloadResult {

    private byte[] content;
    private String contentType;
    private String fileName;
}
