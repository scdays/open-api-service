package com.vtc.openapi.domain.open.service.business;

import com.botany.spore.core.page.PageInfo;
import com.vtc.openapi.domain.open.model.InvocationContext;
import com.vtc.openapi.domain.open.model.entity.ApiInvocationDO;
import com.vtc.openapi.domain.open.model.entity.WebhookDeliveryLogDO;
import com.vtc.openapi.domain.open.model.query.InvocationAdminQuery;
import com.vtc.openapi.domain.open.model.query.WebhookDeliveryLogQuery;
import com.vtc.openapi.domain.open.model.result.PartnerInvocationStatsResult;
import com.vtc.openapi.domain.open.model.result.PartnerQuotaStatResult;

import java.util.Date;

/**
 * API 调用审计（api_invocation）领域服务。
 */
public interface IInvocationDomainService {

    void start(InvocationContext ctx);

    void finish(InvocationContext ctx, int responseCode, String errorMessage);

    PageInfo<ApiInvocationDO> pageInvocations(InvocationAdminQuery query);

    PageInfo<WebhookDeliveryLogDO> pageWebhookDeliveries(WebhookDeliveryLogQuery query);

    PartnerInvocationStatsResult queryPartnerStats(String partnerId);

    PartnerQuotaStatResult queryPartnerQuotaStats(String partnerId, Date startedFrom, Date startedTo);
}
