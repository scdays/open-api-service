package com.vtc.openapi.ui.admin;

import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.ui.BaseUI;
import com.vtc.openapi.app.service.IInvocationAdminAppService;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/admin")
@Validated
@Api(tags = "调用治理内部管理")
public class InvocationAdminUI extends BaseUI {

    private final IInvocationAdminAppService invocationAdminAppService;

    public InvocationAdminUI(IInvocationAdminAppService invocationAdminAppService) {
        this.invocationAdminAppService = invocationAdminAppService;
    }

    @ApiOperation("分页查询调用记录")
    @GetMapping("/invocations")
    public ApiResponse<InvocationPageDto> listInvocations(
            @RequestParam(value = "partnerId", required = false) String partnerId,
            @RequestParam(value = "operationId", required = false) String operationId,
            @RequestParam(value = "domain", required = false) String domain,
            @RequestParam(value = "response_code", required = false) Integer responseCode,
            @RequestParam(value = "resourceType", required = false) String resourceType,
            @RequestParam(value = "resourceId", required = false) String resourceId,
            @RequestParam(value = "startedFrom", required = false) String startedFrom,
            @RequestParam(value = "startedTo", required = false) String startedTo,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        PageInfo<InvocationDTO> pageInfo = getPageInfo(page, size);
        return invocationAdminAppService.listInvocations(
                pageInfo, partnerId, operationId, domain, responseCode, resourceType, resourceId, startedFrom, startedTo);
    }

    @ApiOperation("查询单条调用详情")
    @GetMapping("/invocations/{invocationId}")
    public ApiResponse<InvocationDetailDTO> getInvocationDetail(
            @PathVariable("invocationId") String invocationId) {
        return invocationAdminAppService.getInvocationDetail(invocationId);
    }

    @ApiOperation("按需获取调用响应报文（大报文二次加载）")
    @GetMapping("/invocations/{invocationId}/response-body")
    public ApiResponse<InvocationResponseBodyDTO> getInvocationResponseBody(
            @PathVariable("invocationId") String invocationId) {
        return invocationAdminAppService.getInvocationResponseBody(invocationId);
    }

    @ApiOperation("分页查询 Webhook 投递日志")
    @GetMapping("/webhook-deliveries")
    public ApiResponse<WebhookDeliveryLogPageDto> listWebhookDeliveries(
            @RequestParam(value = "partnerId", required = false) String partnerId,
            @RequestParam(value = "eventType", required = false) String eventType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "resourceType", required = false) String resourceType,
            @RequestParam(value = "resourceId", required = false) String resourceId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        PageInfo<WebhookDeliveryLogDTO> pageInfo = getPageInfo(page, size);
        return invocationAdminAppService.listWebhookDeliveries(
                pageInfo, partnerId, eventType, status, resourceType, resourceId);
    }

    @ApiOperation("查询 Webhook 投递详情")
    @GetMapping("/webhook-deliveries/{deliveryId}")
    public ApiResponse<WebhookDeliveryLogDetailDTO> getWebhookDeliveryDetail(
            @PathVariable("deliveryId") Long deliveryId) {
        return invocationAdminAppService.getWebhookDeliveryDetail(deliveryId);
    }

    @ApiOperation("手动重试 Webhook 投递")
    @PostMapping("/webhook-deliveries/{deliveryId}/retry")
    public ApiResponse<WebhookDeliveryLogDTO> retryWebhookDelivery(
            @PathVariable("deliveryId") Long deliveryId) {
        return invocationAdminAppService.retryWebhookDelivery(deliveryId);
    }

    @ApiOperation("分页查询 Partner 配额与调用统计")
    @GetMapping("/quotas")
    public ApiResponse<PartnerQuotaPageDto> listPartnerQuotas(
            @RequestParam(value = "partnerId", required = false) String partnerId,
            @RequestParam(value = "partnerName", required = false) String partnerName,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startedFrom", required = false) String startedFrom,
            @RequestParam(value = "startedTo", required = false) String startedTo,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        PageInfo<PartnerQuotaDTO> pageInfo = getPageInfo(page, size);
        return invocationAdminAppService.listPartnerQuotas(
                pageInfo, partnerId, partnerName, status, startedFrom, startedTo);
    }
}
