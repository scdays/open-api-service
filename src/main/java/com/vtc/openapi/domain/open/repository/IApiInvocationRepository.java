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

    PageInfo<WebhookDeliveryLogDO> pageWebhookDeliveries(WebhookDeliveryLogQuery query);

    long countByPartnerAndTimeRange(String partnerId, Date from, Date to);

    long countSuccessByPartnerAndTimeRange(String partnerId, Date from, Date to);

    List<InvocationErrorCodeStat> listTopErrorCodes(String partnerId, Date from, Date to, int limit);

    List<InvocationDailyTrendStat> listDailyStats(String partnerId, Date from, Date to);

    PartnerQuotaStatResult summarizePartnerQuota(String partnerId, Date from, Date to);
}
