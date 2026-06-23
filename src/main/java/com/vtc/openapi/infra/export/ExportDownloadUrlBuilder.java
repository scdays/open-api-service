package com.vtc.openapi.infra.export;

import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.infra.config.OpenApiProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class ExportDownloadUrlBuilder {

    private static final String MODE_FILE_SHARING = "file-sharing";
    private static final String MODE_OPEN_API_PATH = "open-api-path";
    private static final String FILE_SHARING_DOWNLOAD_PATH = "/file-sharing-center/file-sharing/download";

    private final OpenApiProperties properties;

    public ExportDownloadUrlBuilder(OpenApiProperties properties) {
        this.properties = properties;
    }

    /**
     * @param exportId 外发 ID（open-api 模式必填）
     * @param bucket   file-sharing 模式使用
     * @param fileKey  file-sharing 模式使用
     */
    public String build(String exportId, String bucket, String fileKey) {
        String mode = properties.getExport().getDownloadUrlMode();
        if (MODE_FILE_SHARING.equalsIgnoreCase(mode)) {
            return buildFileSharingUrl(bucket, fileKey);
        }
        if (MODE_OPEN_API_PATH.equalsIgnoreCase(mode)) {
            return buildOpenApiExportPath(exportId);
        }
        return buildOpenApiExportUrl(exportId);
    }

    public String buildArtifact(String artifactId, String bucket, String fileKey) {
        String mode = properties.getExport().getDownloadUrlMode();
        if (MODE_FILE_SHARING.equalsIgnoreCase(mode)) {
            return buildFileSharingUrl(bucket, fileKey);
        }
        if (MODE_OPEN_API_PATH.equalsIgnoreCase(mode)) {
            return buildOpenApiArtifactPath(artifactId);
        }
        return buildOpenApiArtifactUrl(artifactId);
    }

    private String buildOpenApiExportPath(String exportId) {
        return OpenApiConstants.API_PREFIX + "/exports/" + exportId + "/download";
    }

    private String buildOpenApiArtifactPath(String artifactId) {
        return OpenApiConstants.API_PREFIX + "/artifacts/" + artifactId + "/download";
    }

    private String buildOpenApiExportUrl(String exportId) {
        String baseUrl = normalizeBaseUrl(properties.getPartnerGateway().getPublicBaseUrl(), "http://127.0.0.1:35770");
        return baseUrl + buildOpenApiExportPath(exportId);
    }

    private String buildOpenApiArtifactUrl(String artifactId) {
        String baseUrl = normalizeBaseUrl(properties.getPartnerGateway().getPublicBaseUrl(), "http://127.0.0.1:35770");
        return baseUrl + buildOpenApiArtifactPath(artifactId);
    }

    private String buildFileSharingUrl(String bucket, String fileKey) {
        String baseUrl = normalizeBaseUrl(properties.getFileSharing().getPublicBaseUrl(), "http://127.0.0.1");
        String username = properties.getFileSharing().getDownloadUsername();
        return baseUrl + FILE_SHARING_DOWNLOAD_PATH
                + "?bucket=" + encode(bucket)
                + "&fileKey=" + encode(fileKey)
                + "&_username=" + encode(username);
    }

    private static String normalizeBaseUrl(String baseUrl, String defaultUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            baseUrl = defaultUrl;
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ex) {
            return value;
        }
    }
}
