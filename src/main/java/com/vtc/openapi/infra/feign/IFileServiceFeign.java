package com.vtc.openapi.infra.feign;

import com.botany.spore.core.result.Result;
import com.vtc.openapi.infra.feign.fallback.FileServiceClientError;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "${vtc.application.name.file:file-sharing-center}", path = "/v1/api",
        fallbackFactory = FileServiceClientError.class)
public interface IFileServiceFeign {

    @GetMapping("/read/bytes")
    byte[] readBytes(@RequestParam(name = "bucket") String bucket,
                     @RequestParam(name = "url", required = false) String url,
                     @RequestParam(name = "fileKey") String fileKey);

    @PutMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            headers = "content-type=application/octet-stream")
    Result<String> upload(@RequestParam(name = "bucket") String bucket,
                          @RequestPart(name = "multipartFile") MultipartFile multipartFile);
}
