package com.vtc.openapi.infra.adapter.taskcenter;

import org.springframework.util.StringUtils;

/**
 * 解析 VTC download_report_finish 下发的 SFTP 全路径。
 */
final class TaskCenterReportPathSupport {

    private TaskCenterReportPathSupport() {
    }

    static ParsedReportPath parse(String downloadPath) {
        if (!StringUtils.hasText(downloadPath)) {
            return null;
        }
        String normalized = downloadPath.trim().replace('\\', '/');
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        int slash = normalized.lastIndexOf('/');
        if (slash < 0) {
            return new ParsedReportPath("/", normalized);
        }
        String remoteDir = slash == 0 ? "/" : normalized.substring(0, slash);
        String fileName = normalized.substring(slash + 1);
        if (!StringUtils.hasText(fileName)) {
            return null;
        }
        return new ParsedReportPath(remoteDir, fileName);
    }

    static String buildArchiveFileName(String subId, String originalName) {
        String safeSub = subId != null ? subId.replaceAll("[^A-Za-z0-9_-]", "_") : "sub";
        String safeName = StringUtils.hasText(originalName) ? originalName : "report.xml";
        return "scan-report-" + safeSub + "-" + safeName;
    }

    static final class ParsedReportPath {
        private final String remoteDir;
        private final String fileName;

        ParsedReportPath(String remoteDir, String fileName) {
            this.remoteDir = remoteDir;
            this.fileName = fileName;
        }

        String getRemoteDir() {
            return remoteDir;
        }

        String getFileName() {
            return fileName;
        }
    }
}
