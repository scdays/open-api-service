package com.vtc.openapi.infra.feign;

import com.vtc.openapi.infra.feign.dto.VulPassDispatchRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * vul-pass 扫描任务真实 REST（Read 自 {@code VulScanTaskUi}）。
 * <p>开放平台逻辑契约 {@code POST /task/create}、{@code GET /task/progress} 由
 * {@link com.vtc.openapi.infra.adapter.SvmpEngineAdapterImpl} 翻译为本接口。</p>
 */
@FeignClient(name = "${open-api.svmp.engine-service-name:vul-pass}",
        contextId = "openApiVulPassScanTaskFeign",
        path = "${open-api.svmp.engine-path-prefix:}")
public interface IVulPassScanTaskFeign {

    /** vul-pass {@code POST /vul-scan-task/dispatch} */
    @PostMapping("/vul-scan-task/dispatch")
    String dispatch(@RequestBody VulPassDispatchRequest request);

    /** vul-pass {@code GET /vul-scan-task/page2}，按主任务 id 查进度 */
    @GetMapping("/vul-scan-task/page2")
    String pageTasks(@RequestParam("current") long current,
                     @RequestParam("size") long size,
                     @RequestParam(value = "id", required = false) Long id,
                     @RequestParam(value = "orderId", required = false) String orderId);
}
