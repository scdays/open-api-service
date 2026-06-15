package com.vtc.openapi.infra.export;

import com.botany.spore.core.result.Result;
import com.vtc.core.file.VtcMultipartFile;
import com.vtc.openapi.infra.feign.IFileServiceFeign;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

@Component
public class ExportFileStorageAdapter {

    private static final Logger log = LoggerFactory.getLogger(ExportFileStorageAdapter.class);

    private final IFileServiceFeign fileServiceFeign;
    private final String bucket;

    public ExportFileStorageAdapter(IFileServiceFeign fileServiceFeign,
                                    @Value("${spring.application.name:open-api-service}") String bucket) {
        this.fileServiceFeign = fileServiceFeign;
        this.bucket = bucket;
    }

    public String getBucket() {
        return bucket;
    }

    public String upload(byte[] content, String fileName) {
        try {
            MultipartFile multipartFile = new VtcMultipartFile(
                    fileName, fileName, ContentType.APPLICATION_OCTET_STREAM.getMimeType(),
                    new ByteArrayInputStream(content));
            Result<String> result = fileServiceFeign.upload(bucket, multipartFile);
            if (result != null && Boolean.TRUE.equals(result.getSuccess())) {
                String fileKey = resolveFileKey(result, fileName);
                log.info("export file uploaded: bucket={} fileKey={}", bucket, fileKey);
                return fileKey;
            }
            String msg = result != null ? result.getMessage() : "upload failed";
            throw new IllegalStateException("?????????: " + msg);
        } catch (Exception ex) {
            throw new IllegalStateException("????????: " + ex.getMessage(), ex);
        }
    }

    public byte[] read(String fileKey) {
        byte[] bytes = fileServiceFeign.readBytes(bucket, null, fileKey);
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("?????????: " + fileKey);
        }
        return bytes;
    }

    private static String resolveFileKey(Result<String> result, String fileName) {
        if (result.getData() != null) {
            String data = String.valueOf(result.getData()).trim();
            if (StringUtils.hasText(data)) {
                return data;
            }
        }
        String message = result.getMessage();
        if (StringUtils.hasText(message) && !looksLikeSuccessMessage(message)) {
            return message.trim();
        }
        return fileName;
    }

    private static boolean looksLikeSuccessMessage(String message) {
        String lower = message.toLowerCase();
        return message.contains("???") || lower.contains("success");
    }
}
