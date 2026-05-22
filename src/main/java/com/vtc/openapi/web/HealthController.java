package com.vtc.openapi.web;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@Api(tags = "健康检查")
@RestController
public class HealthController {

    @ApiOperation("K8s 健康检查")
    @GetMapping("/internal/health")
    public Map<String, String> health() {
        return Collections.singletonMap("status", "UP");
    }
}
