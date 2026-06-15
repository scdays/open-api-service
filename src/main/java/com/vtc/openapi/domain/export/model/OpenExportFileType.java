package com.vtc.openapi.domain.export.model;

public final class OpenExportFileType {

    public static final int EXPORT_XML = 11;
    public static final int EXPORT_JSON = 12;

    private OpenExportFileType() {
    }

    public static int fromFormat(String format) {
        return "json".equalsIgnoreCase(format) ? EXPORT_JSON : EXPORT_XML;
    }
}
