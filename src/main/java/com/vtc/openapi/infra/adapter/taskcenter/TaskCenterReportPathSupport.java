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

    /**
     * 由归档文件名推断产物格式（小写扩展名），未匹配默认 xml（vuln 原始报告为 XML）。
     */
    static String inferFileFormat(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "xml";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "xml";
        }
        String ext = fileName.substring(dot + 1).toLowerCase();
        if ("xml".equals(ext) || "xlsx".equals(ext) || "pdf".equals(ext) || "zip".equals(ext)) {
            return ext;
        }
        return "xml";
    }

    /**
     * 由产物格式映射下载 Content-Type。
     */
    static String contentTypeForFormat(String format) {
        if ("xml".equals(format)) {
            return "application/xml";
        }
        if ("xlsx".equals(format)) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        if ("pdf".equals(format)) {
            return "application/pdf";
        }
        if ("zip".equals(format)) {
            return "application/zip";
        }
        return "application/octet-stream";
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
