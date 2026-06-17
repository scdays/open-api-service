package com.vtc.openapi.infra.feign;

import com.vtc.openapi.infra.feign.dto.VulPassVerifyFixRequest;
import com.vtc.openapi.infra.feign.dto.VulPassVerifyFixResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "${open-api.svmp.engine-service-name:vul-pass}",
        contextId = "vulPassOpenInstanceFeign",
        path = "${open-api.svmp.orchestration.path-prefix:/internal/open/v1}/instances")
public interface IVulPassOpenInstanceFeign {

    @PostMapping("/{vulInfoID}/verify-fix")
    VulPassVerifyFixResponse verifyFix(@PathVariable("vulInfoID") String vulInfoId,
                                       @RequestBody VulPassVerifyFixRequest request);
}
