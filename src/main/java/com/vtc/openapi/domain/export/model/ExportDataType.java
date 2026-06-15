package com.vtc.openapi.domain.export.model;

public final class ExportDataType {

    public static final String MIXED = "MIXED";
    public static final String SYSTEM_VULNERABILITY = "SYSTEM_VULNERABILITY";
    public static final String LIVE_PROBE = "LIVE_PROBE";
    public static final String PORT_SCAN = "PORT_SCAN";

    private ExportDataType() {
    }

    public static String fromScanTemplateId(Integer scanTemplateId) {
        if (scanTemplateId == null) {
            return MIXED;
        }
        switch (scanTemplateId) {
            case 1002:
                return LIVE_PROBE;
            case 1003:
                return PORT_SCAN;
            case 1001:
                return SYSTEM_VULNERABILITY;
            default:
                return MIXED;
        }
    }
}
