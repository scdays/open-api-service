package com.vtc.openapi.domain.export.model.result;

import lombok.Data;

@Data
public class ExportDownloadResult {

    private byte[] content;
    private String contentType;
    private String fileName;
}
