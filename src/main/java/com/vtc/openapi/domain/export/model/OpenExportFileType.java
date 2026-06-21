package com.vtc.openapi.domain.export.model;

public final class OpenExportFileType {

    public static final int EXPORT_XML = 11;
    public static final int EXPORT_JSON = 12;
    public static final int EXPORT_ZIP = 13;
    public static final int EXPORT_XLSX = 14;
    public static final int EXPORT_PDF = 15;

    private OpenExportFileType() {
    }

    public static int fromFormat(String format) {
        if (format == null) {
            return EXPORT_XML;
        }
        switch (format.toLowerCase()) {
            case "json":
                return EXPORT_JSON;
            case "zip":
                return EXPORT_ZIP;
            case "xlsx":
                return EXPORT_XLSX;
            case "pdf":
                return EXPORT_PDF;
            default:
                return EXPORT_XML;
        }
    }
}
