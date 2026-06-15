package com.vtc.openapi.infra.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * vul-pass 实例 Feign（Read 自 {@code VulScanTaskSubSystemUi}）。
 */
@FeignClient(name = "${open-api.svmp.engine-service-name:vul-pass}",
        path = "${open-api.svmp.engine-path-prefix:}")
public interface IVulPassInstanceFeign {

    @GetMapping("/vul-scan-task-sub-system/page")
    String pageInstances(@SpringQueryMap Map<String, Object> params);

    @PutMapping("/vul-scan-task-sub-system")
    String updateInstance(@RequestBody Map<String, Object> body);
}
