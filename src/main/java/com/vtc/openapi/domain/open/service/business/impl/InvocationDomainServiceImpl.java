package com.vtc.openapi.domain.open.service.business.impl;

import com.botany.spore.core.page.PageInfo;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.open.model.InvocationContext;
import com.vtc.openapi.domain.open.model.entity.ApiInvocationDO;
import com.vtc.openapi.domain.open.model.query.InvocationAdminQuery;
import com.vtc.openapi.domain.open.model.entity.WebhookDeliveryLogDO;
import com.vtc.openapi.domain.open.model.query.WebhookDeliveryLogQuery;
import com.vtc.openapi.domain.open.model.result.InvocationDailyTrendStat;
import com.vtc.openapi.domain.open.model.result.PartnerInvocationStatsResult;
import com.vtc.openapi.domain.open.model.result.PartnerQuotaStatResult;
import com.vtc.openapi.domain.open.repository.IApiInvocationRepository;
import com.vtc.openapi.domain.partner.service.business.IPartnerDomainService;
import com.vtc.openapi.domain.open.service.business.IInvocationDomainService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InvocationDomainServiceImpl implements IInvocationDomainService {

    private static final int TREND_DAYS = 7;
    private static final int TOP_ERROR_CODE_SIZE = 5;

    private final IApiInvocationRepository apiInvocationRepository;
    private final IPartnerDomainService partnerDomainService;

    public InvocationDomainServiceImpl(IApiInvocationRepository apiInvocationRepository,
                                       IPartnerDomainService partnerDomainService) {
        this.apiInvocationRepository = apiInvocationRepository;
        this.partnerDomainService = partnerDomainService;
    }

    @Override
    public void start(InvocationContext ctx) {
        String invocationId = "INV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        ctx.setInvocationId(invocationId);

        ApiInvocationDO row = new ApiInvocationDO();
        row.setInvocationId(invocationId);
        row.setRequestId(ctx.getRequestId());
        row.setPartnerId(ctx.getPartnerId());
        row.setOperationId(ctx.getOperationId());
        row.setHttpMethod(ctx.getHttpMethod());
        row.setRequestPath(ctx.getRequestPath());
        row.setClientIp(ctx.getClientIp());
        row.setStartedAt(new Date(ctx.getStartedAtMillis()));
        apiInvocationRepository.insert(row);
    }

    @Override
    public void finish(InvocationContext ctx, int responseCode, String errorMessage) {
        if (!StringUtils.hasText(ctx.getInvocationId())) {
            return;
        }
        int latencyMs = (int) Math.min(Integer.MAX_VALUE,
                System.currentTimeMillis() - ctx.getStartedAtMillis());

        ApiInvocationDO row = new ApiInvocationDO();
        row.setInvocationId(ctx.getInvocationId());
        row.setResponseCode(responseCode);
        row.setHttpStatus(200);
        row.setLatencyMs(latencyMs);
        row.setResourceType(ctx.getResourceType());
        row.setResourceId(ctx.getResourceId());
        row.setFinishedAt(new Date());
        if (responseCode != 0 && StringUtils.hasText(errorMessage)) {
            String msg = errorMessage.length() > 512 ? errorMessage.substring(0, 512) : errorMessage;
            row.setErrorMessage(msg);
        }
        apiInvocationRepository.updateFinish(row);
    }

    @Override
    public PageInfo<ApiInvocationDO> pageInvocations(InvocationAdminQuery query) {
        return apiInvocationRepository.pageInvocations(query);
    }

    @Override
    public PageInfo<WebhookDeliveryLogDO> pageWebhookDeliveries(WebhookDeliveryLogQuery query) {
        return apiInvocationRepository.pageWebhookDeliveries(query);
    }

    @Override
    public PartnerInvocationStatsResult queryPartnerStats(String partnerId) {
        if (!StringUtils.hasText(partnerId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "partnerId 不能为空");
        }
        partnerDomainService.requireByPartnerId(partnerId);

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        Date todayFrom = toDate(today);
        Date tomorrowFrom = toDate(today.plusDays(1));
        LocalDate trendStartDate = today.minusDays(TREND_DAYS - 1L);
        Date trendFrom = toDate(trendStartDate);

        long todayTotal = apiInvocationRepository.countByPartnerAndTimeRange(partnerId, todayFrom, tomorrowFrom);
        long todaySuccess = apiInvocationRepository.countSuccessByPartnerAndTimeRange(partnerId, todayFrom, tomorrowFrom);

        PartnerInvocationStatsResult result = new PartnerInvocationStatsResult();
        result.setPartnerId(partnerId);
        result.setTodayTotal(todayTotal);
        result.setTodaySuccess(todaySuccess);
        result.setTodaySuccessRate(calcRate(todaySuccess, todayTotal));
        result.setTopErrorCodes(apiInvocationRepository.listTopErrorCodes(
                partnerId, trendFrom, tomorrowFrom, TOP_ERROR_CODE_SIZE));
        result.setDailyTrend(completeTrend(
                trendStartDate,
                TREND_DAYS,
                apiInvocationRepository.listDailyStats(partnerId, trendFrom, tomorrowFrom)));
        return result;
    }

    @Override
    public PartnerQuotaStatResult queryPartnerQuotaStats(String partnerId, Date startedFrom, Date startedTo) {
        if (!StringUtils.hasText(partnerId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "partnerId 不能为空");
        }
        return apiInvocationRepository.summarizePartnerQuota(partnerId, startedFrom, startedTo);
    }

    private List<InvocationDailyTrendStat> completeTrend(LocalDate startDate,
                                                         int days,
                                                         List<InvocationDailyTrendStat> actualStats) {
        Map<LocalDate, InvocationDailyTrendStat> byDay = new HashMap<>();
        if (actualStats != null) {
            for (InvocationDailyTrendStat stat : actualStats) {
                byDay.put(stat.getStatDate(), stat);
            }
        }
        List<InvocationDailyTrendStat> full = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            InvocationDailyTrendStat stat = byDay.get(date);
            if (stat == null) {
                stat = new InvocationDailyTrendStat();
                stat.setStatDate(date);
                stat.setTotalCount(0);
                stat.setSuccessCount(0);
            }
            full.add(stat);
        }
        return full;
    }

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private double calcRate(long success, long total) {
        if (total <= 0L) {
            return 0D;
        }
        return ((double) success) / (double) total;
    }
}
