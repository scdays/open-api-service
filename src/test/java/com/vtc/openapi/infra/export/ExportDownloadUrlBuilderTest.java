package com.vtc.openapi.infra.export;

import com.vtc.openapi.infra.config.OpenApiProperties;
import org.junit.Assert;
import org.junit.Test;

public class ExportDownloadUrlBuilderTest {

    @Test
    public void openApiModeBuildsAbsoluteExportUrl() {
        ExportDownloadUrlBuilder builder = builderWithMode("open-api");
        String url = builder.build("EXP-001", "bucket-a", "key-a");
        Assert.assertEquals("http://gateway.example:35770/api/open/v1/exports/EXP-001/download", url);
    }

    @Test
    public void openApiModeBuildsAbsoluteArtifactUrl() {
        ExportDownloadUrlBuilder builder = builderWithMode("open-api");
        String url = builder.buildArtifact("ART-001", "bucket-a", "key-a");
        Assert.assertEquals("http://gateway.example:35770/api/open/v1/artifacts/ART-001/download", url);
    }

    @Test
    public void openApiPathModeBuildsRelativeExportPath() {
        ExportDownloadUrlBuilder builder = builderWithMode("open-api-path");
        String url = builder.build("EXP-002", "bucket-a", "key-a");
        Assert.assertEquals("/api/open/v1/exports/EXP-002/download", url);
    }

    @Test
    public void openApiPathModeBuildsRelativeArtifactPath() {
        ExportDownloadUrlBuilder builder = builderWithMode("open-api-path");
        String url = builder.buildArtifact("ART-002", "bucket-a", "key-a");
        Assert.assertEquals("/api/open/v1/artifacts/ART-002/download", url);
    }

    @Test
    public void fileSharingModeBuildsDirectDownloadUrl() {
        ExportDownloadUrlBuilder builder = builderWithMode("file-sharing");
        String url = builder.build("EXP-003", "bucket-b", "reports/scan.xml");
        Assert.assertTrue(url.startsWith("http://files.example/file-sharing-center/file-sharing/download?"));
        Assert.assertTrue(url.contains("bucket=bucket-b"));
        Assert.assertTrue(url.contains("fileKey=reports%2Fscan.xml"));
        Assert.assertTrue(url.contains("_username=svc-user"));
    }

    private static ExportDownloadUrlBuilder builderWithMode(String mode) {
        OpenApiProperties properties = new OpenApiProperties();
        properties.getExport().setDownloadUrlMode(mode);
        properties.getPartnerGateway().setPublicBaseUrl("http://gateway.example:35770");
        properties.getFileSharing().setPublicBaseUrl("http://files.example");
        properties.getFileSharing().setDownloadUsername("svc-user");
        return new ExportDownloadUrlBuilder(properties);
    }
}
