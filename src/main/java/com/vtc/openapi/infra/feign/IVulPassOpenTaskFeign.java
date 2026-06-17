package com.vtc.openapi.infra.feign;

import com.vtc.openapi.infra.feign.dto.VulPassCreateOpenTaskRequest;
import com.vtc.openapi.infra.feign.dto.VulPassCreateOpenTaskResponse;
import com.vtc.openapi.infra.feign.dto.VulPassOpenTaskProgressResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * vul-pass OPEN 编排内部 API（{@code /internal/open/v1}）。
 */
@FeignClient(name = "${open-api.svmp.engine-service-name:vul-pass}",
        contextId = "vulPassOpenTaskFeign",
        path = "${open-api.svmp.orchestration.path-prefix:/internal/open/v1}")
public interface IVulPassOpenTaskFeign {

    @PostMapping("/tasks")
    VulPassCreateOpenTaskResponse createTask(@RequestBody VulPassCreateOpenTaskRequest request);

    @GetMapping("/tasks/{passTaskId}")
    VulPassOpenTaskProgressResponse getTask(@PathVariable("passTaskId") Long passTaskId);
}
