package com.vtc.openapi.infra.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.infra.utils.convertor.ConvertHelper;
import com.vtc.openapi.domain.open.model.entity.ApiInvocationDO;
import com.vtc.openapi.domain.open.model.query.InvocationAdminQuery;
import com.vtc.openapi.domain.open.model.entity.WebhookDeliveryLogDO;
import com.vtc.openapi.domain.open.model.query.WebhookDeliveryLogQuery;
import com.vtc.openapi.domain.open.model.result.InvocationDailyTrendStat;
import com.vtc.openapi.domain.open.model.result.InvocationErrorCodeStat;
import com.vtc.openapi.domain.open.model.result.PartnerQuotaStatResult;
import com.vtc.openapi.domain.open.repository.IApiInvocationRepository;
import com.vtc.openapi.infra.dao.ApiInvocationMapper;
import com.vtc.openapi.infra.dao.WebhookDeliveryLogMapper;
import com.vtc.openapi.infra.dao.data.InvocationDailyStatRow;
import com.vtc.openapi.infra.dao.data.InvocationErrorCodeStatRow;
import com.vtc.openapi.infra.dao.po.ApiInvocationPO;
import com.vtc.openapi.infra.dao.po.WebhookDeliveryLogPO;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class ApiInvocationRepositoryImpl implements IApiInvocationRepository {

    private final ApiInvocationMapper apiInvocationMapper;
    private final WebhookDeliveryLogMapper webhookDeliveryLogMapper;

    public ApiInvocationRepositoryImpl(ApiInvocationMapper apiInvocationMapper,
                                       WebhookDeliveryLogMapper webhookDeliveryLogMapper) {
        this.apiInvocationMapper = apiInvocationMapper;
        this.webhookDeliveryLogMapper = webhookDeliveryLogMapper;
    }

    @Override
    public void insert(ApiInvocationDO invocation) {
        apiInvocationMapper.insert(ConvertHelper.convert(invocation, ApiInvocationPO.class));
    }

    @Override
    public void updateFinish(ApiInvocationDO patch) {
        if (!StringUtils.hasText(patch.getInvocationId())) {
            return;
        }
        ApiInvocationPO row = new ApiInvocationPO();
        row.setInvocationId(patch.getInvocationId());
        row.setResponseCode(patch.getResponseCode());
        row.setHttpStatus(patch.getHttpStatus());
        row.setLatencyMs(patch.getLatencyMs());
        row.setResourceType(patch.getResourceType());
        row.setResourceId(patch.getResourceId());
        row.setFinishedAt(patch.getFinishedAt());
        row.setErrorMessage(patch.getErrorMessage());
        apiInvocationMapper.updateById(row);
    }

    @Override
    public PageInfo<ApiInvocationDO> pageInvocations(InvocationAdminQuery query) {
        LambdaQueryWrapper<ApiInvocationPO> wrapper = new LambdaQueryWrapper<ApiInvocationPO>()
                .orderByDesc(ApiInvocationPO::getStartedAt);
        if (StringUtils.hasText(query.getPartnerId())) {
            wrapper.eq(ApiInvocationPO::getPartnerId, query.getPartnerId());
        }
        if (StringUtils.hasText(query.getOperationId())) {
            wrapper.eq(ApiInvocationPO::getOperationId, query.getOperationId());
        }
        if (query.getResponseCode() != null) {
            wrapper.eq(ApiInvocationPO::getResponseCode, query.getResponseCode());
        }
        if (query.getStartedFrom() != null) {
            wrapper.ge(ApiInvocationPO::getStartedAt, query.getStartedFrom());
        }
        if (query.getStartedTo() != null) {
            wrapper.le(ApiInvocationPO::getStartedAt, query.getStartedTo());
        }
        Page<ApiInvocationPO> pageResult = apiInvocationMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper);

        PageInfo<ApiInvocationDO> pageInfo = new PageInfo<>();
        pageInfo.setCurrent(query.getPage());
        pageInfo.setSize(query.getSize());
        pageInfo.setTotal(pageResult.getTotal());
        pageInfo.setRecords(ConvertHelper.convertList(pageResult.getRecords(), ApiInvocationDO.class));
        return pageInfo;
    }

    @Override
    public PageInfo<WebhookDeliveryLogDO> pageWebhookDeliveries(WebhookDeliveryLogQuery query) {
        LambdaQueryWrapper<WebhookDeliveryLogPO> wrapper = new LambdaQueryWrapper<WebhookDeliveryLogPO>()
                .orderByDesc(WebhookDeliveryLogPO::getCreatedAt);
        if (StringUtils.hasText(query.getPartnerId())) {
            wrapper.eq(WebhookDeliveryLogPO::getPartnerId, query.getPartnerId());
        }
        if (StringUtils.hasText(query.getEventType())) {
            wrapper.eq(WebhookDeliveryLogPO::getEventType, query.getEventType());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(WebhookDeliveryLogPO::getStatus, query.getStatus());
        }

        Page<WebhookDeliveryLogPO> pageResult = webhookDeliveryLogMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper);

        PageInfo<WebhookDeliveryLogDO> pageInfo = new PageInfo<>();
        pageInfo.setCurrent(query.getPage());
        pageInfo.setSize(query.getSize());
        pageInfo.setTotal(pageResult.getTotal());
        pageInfo.setRecords(ConvertHelper.convertList(pageResult.getRecords(), WebhookDeliveryLogDO.class));
        return pageInfo;
    }

    @Override
    public long countByPartnerAndTimeRange(String partnerId, Date from, Date to) {
        Long count = apiInvocationMapper.countByPartnerAndTimeRange(partnerId, from, to);
        return count == null ? 0L : count;
    }

    @Override
    public long countSuccessByPartnerAndTimeRange(String partnerId, Date from, Date to) {
        Long count = apiInvocationMapper.countSuccessByPartnerAndTimeRange(partnerId, from, to);
        return count == null ? 0L : count;
    }

    @Override
    public List<InvocationErrorCodeStat> listTopErrorCodes(String partnerId, Date from, Date to, int limit) {
        List<InvocationErrorCodeStatRow> rows = apiInvocationMapper.selectTopErrorCodes(partnerId, from, to, limit);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream().map(this::toErrorCodeStat).collect(Collectors.toList());
    }

    @Override
    public List<InvocationDailyTrendStat> listDailyStats(String partnerId, Date from, Date to) {
        List<InvocationDailyStatRow> rows = apiInvocationMapper.selectDailyStats(partnerId, from, to);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream().map(this::toDailyStat).collect(Collectors.toList());
    }

    @Override
    public PartnerQuotaStatResult summarizePartnerQuota(String partnerId, Date from, Date to) {
        long total = safeLong(apiInvocationMapper.countByPartnerWithRange(partnerId, from, to));
        long success = safeLong(apiInvocationMapper.countSuccessByPartnerWithRange(partnerId, from, to));
        long failed = Math.max(0L, total - success);

        PartnerQuotaStatResult result = new PartnerQuotaStatResult();
        result.setPartnerId(partnerId);
        result.setTotalInvocations(total);
        result.setSuccessInvocations(success);
        result.setFailedInvocations(failed);
        result.setSuccessRate(calcRate(success, total));
        return result;
    }

    private InvocationErrorCodeStat toErrorCodeStat(InvocationErrorCodeStatRow row) {
        InvocationErrorCodeStat stat = new InvocationErrorCodeStat();
        stat.setResponseCode(row.getResponseCode());
        stat.setCount(row.getCount() == null ? 0L : row.getCount());
        return stat;
    }

    private InvocationDailyTrendStat toDailyStat(InvocationDailyStatRow row) {
        InvocationDailyTrendStat stat = new InvocationDailyTrendStat();
        stat.setStatDate(row.getStatDay().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        stat.setTotalCount(row.getTotalCount() == null ? 0L : row.getTotalCount());
        stat.setSuccessCount(row.getSuccessCount() == null ? 0L : row.getSuccessCount());
        return stat;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private double calcRate(long success, long total) {
        if (total <= 0L) {
            return 0D;
        }
        return ((double) success) / (double) total;
    }
}
