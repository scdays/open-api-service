package com.vtc.openapi.ui.admin;

import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.ui.BaseUI;
import com.vtc.openapi.app.service.IInvocationAdminAppService;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.InvocationDTO;
import com.vtc.openapi.ui.dto.admin.InvocationPageDto;
import com.vtc.openapi.ui.dto.admin.PartnerQuotaDTO;
import com.vtc.openapi.ui.dto.admin.PartnerQuotaPageDto;
import com.vtc.openapi.ui.dto.admin.WebhookDeliveryLogDTO;
import com.vtc.openapi.ui.dto.admin.WebhookDeliveryLogPageDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
            @RequestParam(value = "response_code", required = false) Integer responseCode,
            @RequestParam(value = "startedFrom", required = false) String startedFrom,
            @RequestParam(value = "startedTo", required = false) String startedTo,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        PageInfo<InvocationDTO> pageInfo = getPageInfo(page, size);
        return invocationAdminAppService.listInvocations(
                pageInfo, partnerId, operationId, responseCode, startedFrom, startedTo);
    }

    @ApiOperation("分页查询 Webhook 投递日志")
    @GetMapping("/webhook-deliveries")
    public ApiResponse<WebhookDeliveryLogPageDto> listWebhookDeliveries(
            @RequestParam(value = "partnerId", required = false) String partnerId,
            @RequestParam(value = "eventType", required = false) String eventType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        PageInfo<WebhookDeliveryLogDTO> pageInfo = getPageInfo(page, size);
        return invocationAdminAppService.listWebhookDeliveries(pageInfo, partnerId, eventType, status);
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
