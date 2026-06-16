package com.vtc.openapi.app.service;

import com.botany.spore.core.page.PageInfo;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.*;

public interface IInvocationAdminAppService {

    ApiResponse<InvocationPageDto> listInvocations(PageInfo<InvocationDTO> pageInfo,
                                                   String partnerId,
                                                   String operationId,
                                                   String domain,
                                                   Integer responseCode,
                                                   String resourceType,
                                                   String resourceId,
                                                   String startedFrom,
                                                   String startedTo);

    ApiResponse<InvocationDetailDTO> getInvocationDetail(String invocationId);

    ApiResponse<InvocationResponseBodyDTO> getInvocationResponseBody(String invocationId);

    ApiResponse<InvocationRequestBodyDTO> getInvocationRequestBody(String invocationId);

    ApiResponse<PartnerInvocationStatsDto> getPartnerStats(String partnerId);

    ApiResponse<PartnerQuotaPageDto> listPartnerQuotas(PageInfo<PartnerQuotaDTO> pageInfo,
                                                       String partnerId,
                                                       String partnerName,
                                                       String status,
                                                       String startedFrom,
                                                       String startedTo);

    ApiResponse<WebhookDeliveryLogPageDto> listWebhookDeliveries(PageInfo<WebhookDeliveryLogDTO> pageInfo,
                                                                 String partnerId,
                                                                 String eventType,
                                                                 String status,
                                                                 String resourceType,
                                                                 String resourceId);

    ApiResponse<WebhookDeliveryLogDetailDTO> getWebhookDeliveryDetail(Long deliveryId);

    ApiResponse<WebhookDeliveryLogDTO> retryWebhookDelivery(Long deliveryId);
}
