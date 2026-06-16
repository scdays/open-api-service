package com.vtc.openapi.domain.open.repository;

import com.botany.spore.core.page.PageInfo;
import com.vtc.openapi.domain.open.model.entity.ApiInvocationDO;
import com.vtc.openapi.domain.open.model.entity.WebhookDeliveryLogDO;
import com.vtc.openapi.domain.open.model.query.InvocationAdminQuery;
import com.vtc.openapi.domain.open.model.query.WebhookDeliveryLogQuery;
import com.vtc.openapi.domain.open.model.result.InvocationDailyTrendStat;
import com.vtc.openapi.domain.open.model.result.InvocationErrorCodeStat;
import com.vtc.openapi.domain.open.model.result.PartnerQuotaStatResult;

import java.util.Date;
import java.util.List;

public interface IApiInvocationRepository {

    void insert(ApiInvocationDO invocation);

    void updateFinish(ApiInvocationDO patch);

    PageInfo<ApiInvocationDO> pageInvocations(InvocationAdminQuery query);

    ApiInvocationDO findByInvocationId(String invocationId);

    ApiInvocationDO findByRequestId(String requestId);

    List<WebhookDeliveryLogDO> listWebhookDeliveriesNear(String partnerId, Date from, Date to, int limit);

    WebhookDeliveryLogDO findWebhookDeliveryById(Long id);

    List<WebhookDeliveryLogDO> listWebhookDeliveriesByPartnerEventNear(
            String partnerId, String eventType, Date from, Date to, int limit);

    List<WebhookDeliveryLogDO> listByEventId(String partnerId, String eventId);

    List<WebhookDeliveryLogDO> listByResource(String partnerId, String resourceType, String resourceId, int limit);

    List<ApiInvocationDO> listInvocationsByResource(String partnerId, String resourceType, String resourceId, int limit);

    PageInfo<WebhookDeliveryLogDO> pageWebhookDeliveries(WebhookDeliveryLogQuery query);

    long countByPartnerAndTimeRange(String partnerId, Date from, Date to);

    long countSuccessByPartnerAndTimeRange(String partnerId, Date from, Date to);

    List<InvocationErrorCodeStat> listTopErrorCodes(String partnerId, Date from, Date to, int limit);

    List<InvocationDailyTrendStat> listDailyStats(String partnerId, Date from, Date to);

    PartnerQuotaStatResult summarizePartnerQuota(String partnerId, Date from, Date to);

    String findResponseBodyJson(String invocationId);

    long findResponseBodyByteSize(String invocationId);

    String findRequestBodyJson(String invocationId);

    long findRequestBodyByteSize(String invocationId);
}
