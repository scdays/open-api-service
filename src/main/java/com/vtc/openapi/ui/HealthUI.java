package com.vtc.openapi.ui;

import com.vtc.openapi.infra.config.OpenApiProperties;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Api(tags = "健康检查")
@RestController
public class HealthUI {

    private final OpenApiProperties openApiProperties;

    public HealthUI(OpenApiProperties openApiProperties) {
        this.openApiProperties = openApiProperties;
    }

    @ApiOperation("K8s 健康检查")
    @GetMapping("/internal/health")
    public Map<String, String> health() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "UP");
        if (openApiProperties != null && openApiProperties.getEngine() != null) {
            body.put("adapterMode", openApiProperties.getEngine().getAdapterMode());
        }
        return body;
    }
}
