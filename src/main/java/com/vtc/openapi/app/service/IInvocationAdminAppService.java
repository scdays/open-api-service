package com.vtc.openapi.app.service;

import com.botany.spore.core.page.PageInfo;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.InvocationDTO;
import com.vtc.openapi.ui.dto.admin.InvocationPageDto;
import com.vtc.openapi.ui.dto.admin.PartnerInvocationStatsDto;
import com.vtc.openapi.ui.dto.admin.PartnerQuotaDTO;
import com.vtc.openapi.ui.dto.admin.PartnerQuotaPageDto;
import com.vtc.openapi.ui.dto.admin.WebhookDeliveryLogDTO;
import com.vtc.openapi.ui.dto.admin.WebhookDeliveryLogPageDto;

public interface IInvocationAdminAppService {

    ApiResponse<InvocationPageDto> listInvocations(PageInfo<InvocationDTO> pageInfo,
                                                   String partnerId,
                                                   String operationId,
                                                   Integer responseCode,
                                                   String startedFrom,
                                                   String startedTo);

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
                                                                 String status);
}
