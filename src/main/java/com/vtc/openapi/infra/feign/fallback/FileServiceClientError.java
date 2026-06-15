package com.vtc.openapi.infra.feign.fallback;

import com.botany.spore.core.result.Result;
import com.vtc.openapi.infra.feign.IFileServiceFeign;
import feign.hystrix.FallbackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileServiceClientError implements FallbackFactory<IFileServiceFeign> {

    private static final Logger log = LoggerFactory.getLogger(FileServiceClientError.class);

    @Override
    public IFileServiceFeign create(Throwable cause) {
        log.warn("file-sharing-center 不可用: {}", cause.getMessage());
        return new IFileServiceFeign() {
            @Override
            public byte[] readBytes(String bucket, String url, String fileKey) {
                return new byte[0];
            }

            @Override
            public Result<String> upload(String bucket, MultipartFile multipartFile) {
                return Result.failure("file-sharing-center unavailable");
            }
        };
    }
}
