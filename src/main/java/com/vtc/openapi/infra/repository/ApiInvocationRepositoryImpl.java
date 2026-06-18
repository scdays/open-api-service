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
import com.vtc.openapi.domain.open.model.support.InvocationDomainSupport;
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
        row.setCaseId(patch.getCaseId());
        row.setFinishedAt(patch.getFinishedAt());
        row.setErrorMessage(patch.getErrorMessage());
        row.setResponseBodyJson(patch.getResponseBodyJson());
        apiInvocationMapper.updateById(row);
    }

    @Override
    public void updateCaseId(String invocationId, String caseId) {
        if (!StringUtils.hasText(invocationId) || !StringUtils.hasText(caseId)) {
            return;
        }
        ApiInvocationPO row = new ApiInvocationPO();
        row.setInvocationId(invocationId);
        row.setCaseId(caseId);
        apiInvocationMapper.updateById(row);
    }

    @Override
    public List<ApiInvocationDO> listByCaseId(String caseId, int limit) {
        if (!StringUtils.hasText(caseId)) {
            return Collections.emptyList();
        }
        int capped = limit > 0 ? Math.min(limit, 100) : 20;
        LambdaQueryWrapper<ApiInvocationPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiInvocationPO::getCaseId, caseId);
        wrapper.orderByDesc(ApiInvocationPO::getStartedAt);
        wrapper.last("LIMIT " + capped);
        List<ApiInvocationPO> rows = apiInvocationMapper.selectList(wrapper);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream()
                .map(po -> ConvertHelper.convert(po, ApiInvocationDO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<ApiInvocationDO> listCaseOperationsWithoutCaseId(String partnerId,
                                                                 List<String> operationIds,
                                                                 int limit) {
        if (operationIds == null || operationIds.isEmpty()) {
            return Collections.emptyList();
        }
        int capped = limit > 0 ? Math.min(limit, 500) : 200;
        LambdaQueryWrapper<ApiInvocationPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ApiInvocationPO::getOperationId, operationIds);
        wrapper.and(w -> w.isNull(ApiInvocationPO::getCaseId).or().eq(ApiInvocationPO::getCaseId, ""));
        if (StringUtils.hasText(partnerId)) {
            wrapper.eq(ApiInvocationPO::getPartnerId, partnerId.trim());
        }
        wrapper.orderByDesc(ApiInvocationPO::getStartedAt);
        wrapper.last("LIMIT " + capped);
        List<ApiInvocationPO> rows = apiInvocationMapper.selectList(wrapper);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return ConvertHelper.convertList(rows, ApiInvocationDO.class);
    }

    @Override
    public PageInfo<ApiInvocationDO> pageInvocations(InvocationAdminQuery query) {
        LambdaQueryWrapper<ApiInvocationPO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getPartnerId())) {
            wrapper.eq(ApiInvocationPO::getPartnerId, query.getPartnerId());
        }
        if (StringUtils.hasText(query.getOperationId())) {
            wrapper.eq(ApiInvocationPO::getOperationId, query.getOperationId());
        }
        if (StringUtils.hasText(query.getDomain())) {
            List<String> operationIds = InvocationDomainSupport.operationIdsForDomain(query.getDomain());
            if (!operationIds.isEmpty()) {
                wrapper.in(ApiInvocationPO::getOperationId, operationIds);
            }
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
        if (StringUtils.hasText(query.getResourceId())) {
            String resourceId = query.getResourceId().trim();
            wrapper.eq(ApiInvocationPO::getResourceId, resourceId);
        } else if (StringUtils.hasText(query.getResourceType())) {
            wrapper.eq(ApiInvocationPO::getResourceType, query.getResourceType());
        }
        wrapper.orderByDesc(ApiInvocationPO::getStartedAt)
                .orderByDesc(ApiInvocationPO::getFinishedAt);
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
    public List<ApiInvocationDO> listRecentByPartnerAndOperations(String partnerId, List<String> operationIds,
                                                                  Integer responseCode, int limit) {
        if (!StringUtils.hasText(partnerId) || operationIds == null || operationIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<ApiInvocationPO> wrapper = new LambdaQueryWrapper<ApiInvocationPO>()
                .eq(ApiInvocationPO::getPartnerId, partnerId.trim())
                .in(ApiInvocationPO::getOperationId, operationIds)
                .orderByDesc(ApiInvocationPO::getStartedAt)
                .orderByDesc(ApiInvocationPO::getFinishedAt);
        if (responseCode != null) {
            wrapper.eq(ApiInvocationPO::getResponseCode, responseCode);
        }
        int capped = Math.max(1, Math.min(limit, 500));
        wrapper.last("LIMIT " + capped);
        return ConvertHelper.convertList(apiInvocationMapper.selectList(wrapper), ApiInvocationDO.class);
    }

    @Override
    public ApiInvocationDO findByInvocationId(String invocationId) {
        if (!StringUtils.hasText(invocationId)) {
            return null;
        }
        ApiInvocationPO po = apiInvocationMapper.selectById(invocationId);
        return po == null ? null : ConvertHelper.convert(po, ApiInvocationDO.class);
    }

    @Override
    public ApiInvocationDO findByRequestId(String requestId) {
        if (!StringUtils.hasText(requestId)) {
            return null;
        }
        ApiInvocationPO po = apiInvocationMapper.selectOne(new LambdaQueryWrapper<ApiInvocationPO>()
                .eq(ApiInvocationPO::getRequestId, requestId)
                .orderByDesc(ApiInvocationPO::getStartedAt)
                .last("LIMIT 1"));
        return po == null ? null : ConvertHelper.convert(po, ApiInvocationDO.class);
    }

    @Override
    public List<WebhookDeliveryLogDO> listWebhookDeliveriesNear(String partnerId, Date from, Date to, int limit) {
        if (!StringUtils.hasText(partnerId) || from == null || to == null || limit <= 0) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<WebhookDeliveryLogPO> wrapper = new LambdaQueryWrapper<WebhookDeliveryLogPO>()
                .eq(WebhookDeliveryLogPO::getPartnerId, partnerId)
                .ge(WebhookDeliveryLogPO::getCreatedAt, from)
                .le(WebhookDeliveryLogPO::getCreatedAt, to)
                .orderByAsc(WebhookDeliveryLogPO::getCreatedAt)
                .last("LIMIT " + limit);
        List<WebhookDeliveryLogPO> rows = webhookDeliveryLogMapper.selectList(wrapper);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return ConvertHelper.convertList(rows, WebhookDeliveryLogDO.class);
    }

    @Override
    public WebhookDeliveryLogDO findWebhookDeliveryById(Long id) {
        if (id == null) {
            return null;
        }
        WebhookDeliveryLogPO po = webhookDeliveryLogMapper.selectById(id);
        return po == null ? null : ConvertHelper.convert(po, WebhookDeliveryLogDO.class);
    }

    @Override
    public List<WebhookDeliveryLogDO> listWebhookDeliveriesByPartnerEventNear(
            String partnerId, String eventType, Date from, Date to, int limit) {
        if (!StringUtils.hasText(partnerId) || from == null || to == null || limit <= 0) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<WebhookDeliveryLogPO> wrapper = new LambdaQueryWrapper<WebhookDeliveryLogPO>()
                .eq(WebhookDeliveryLogPO::getPartnerId, partnerId)
                .ge(WebhookDeliveryLogPO::getCreatedAt, from)
                .le(WebhookDeliveryLogPO::getCreatedAt, to)
                .orderByAsc(WebhookDeliveryLogPO::getCreatedAt)
                .last("LIMIT " + limit);
        if (StringUtils.hasText(eventType)) {
            wrapper.eq(WebhookDeliveryLogPO::getEventType, eventType);
        }
        List<WebhookDeliveryLogPO> rows = webhookDeliveryLogMapper.selectList(wrapper);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return ConvertHelper.convertList(rows, WebhookDeliveryLogDO.class);
    }

    @Override
    public List<WebhookDeliveryLogDO> listByEventId(String partnerId, String eventId) {
        if (!StringUtils.hasText(partnerId) || !StringUtils.hasText(eventId)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<WebhookDeliveryLogPO> wrapper = new LambdaQueryWrapper<WebhookDeliveryLogPO>()
                .eq(WebhookDeliveryLogPO::getPartnerId, partnerId)
                .eq(WebhookDeliveryLogPO::getEventId, eventId)
                .orderByAsc(WebhookDeliveryLogPO::getCreatedAt)
                .orderByAsc(WebhookDeliveryLogPO::getId);
        List<WebhookDeliveryLogPO> rows = webhookDeliveryLogMapper.selectList(wrapper);
        if (rows == null || rows.isEmpty()) {
            LambdaQueryWrapper<WebhookDeliveryLogPO> legacyWrapper = new LambdaQueryWrapper<WebhookDeliveryLogPO>()
                    .eq(WebhookDeliveryLogPO::getPartnerId, partnerId)
                    .like(WebhookDeliveryLogPO::getPayloadJson, "\"eventId\":\"" + eventId + "\"")
                    .orderByAsc(WebhookDeliveryLogPO::getCreatedAt)
                    .orderByAsc(WebhookDeliveryLogPO::getId);
            rows = webhookDeliveryLogMapper.selectList(legacyWrapper);
        }
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return ConvertHelper.convertList(rows, WebhookDeliveryLogDO.class);
    }

    @Override
    public List<WebhookDeliveryLogDO> listByResource(String partnerId, String resourceType, String resourceId, int limit) {
        if (!StringUtils.hasText(partnerId) || !StringUtils.hasText(resourceId) || limit <= 0) {
            return Collections.emptyList();
        }
        String trimmedResourceId = resourceId.trim();
        LambdaQueryWrapper<WebhookDeliveryLogPO> wrapper = new LambdaQueryWrapper<WebhookDeliveryLogPO>()
                .eq(WebhookDeliveryLogPO::getPartnerId, partnerId);
        if (StringUtils.hasText(resourceType)) {
            wrapper.eq(WebhookDeliveryLogPO::getResourceType, resourceType);
        }
        wrapper.and(w -> w.eq(WebhookDeliveryLogPO::getResourceId, trimmedResourceId)
                        .or().like(WebhookDeliveryLogPO::getResourceIdsJson, trimmedResourceId))
                .orderByDesc(WebhookDeliveryLogPO::getCreatedAt)
                .orderByDesc(WebhookDeliveryLogPO::getId)
                .last("LIMIT " + limit);
        List<WebhookDeliveryLogPO> rows = webhookDeliveryLogMapper.selectList(wrapper);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return ConvertHelper.convertList(rows, WebhookDeliveryLogDO.class);
    }

    @Override
    public List<ApiInvocationDO> listInvocationsByResource(String partnerId, String resourceType, String resourceId, int limit) {
        if (!StringUtils.hasText(partnerId) || !StringUtils.hasText(resourceId) || limit <= 0) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<ApiInvocationPO> wrapper = new LambdaQueryWrapper<ApiInvocationPO>()
                .eq(ApiInvocationPO::getPartnerId, partnerId)
                .eq(ApiInvocationPO::getResourceId, resourceId.trim())
                .orderByDesc(ApiInvocationPO::getStartedAt)
                .orderByDesc(ApiInvocationPO::getInvocationId)
                .last("LIMIT " + limit);
        if (StringUtils.hasText(resourceType) && !StringUtils.hasText(resourceId)) {
            wrapper.eq(ApiInvocationPO::getResourceType, resourceType);
        }
        List<ApiInvocationPO> rows = apiInvocationMapper.selectList(wrapper);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return ConvertHelper.convertList(rows, ApiInvocationDO.class);
    }

    @Override
    public PageInfo<WebhookDeliveryLogDO> pageWebhookDeliveries(WebhookDeliveryLogQuery query) {
        LambdaQueryWrapper<WebhookDeliveryLogPO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getPartnerId())) {
            wrapper.eq(WebhookDeliveryLogPO::getPartnerId, query.getPartnerId());
        }
        if (StringUtils.hasText(query.getEventType())) {
            wrapper.eq(WebhookDeliveryLogPO::getEventType, query.getEventType());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(WebhookDeliveryLogPO::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getResourceType())) {
            wrapper.eq(WebhookDeliveryLogPO::getResourceType, query.getResourceType());
        }
        if (StringUtils.hasText(query.getResourceId())) {
            String resourceId = query.getResourceId().trim();
            wrapper.and(w -> w.eq(WebhookDeliveryLogPO::getResourceId, resourceId)
                    .or().like(WebhookDeliveryLogPO::getResourceIdsJson, resourceId));
        }
        wrapper.orderByDesc(WebhookDeliveryLogPO::getCreatedAt)
                .orderByDesc(WebhookDeliveryLogPO::getId);

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

    @Override
    public String findResponseBodyJson(String invocationId) {
        if (!StringUtils.hasText(invocationId)) {
            return null;
        }
        return apiInvocationMapper.selectResponseBodyJson(invocationId.trim());
    }

    @Override
    public long findResponseBodyByteSize(String invocationId) {
        if (!StringUtils.hasText(invocationId)) {
            return 0L;
        }
        Long size = apiInvocationMapper.selectResponseBodyByteSize(invocationId.trim());
        return size == null ? 0L : size;
    }

    @Override
    public String findRequestBodyJson(String invocationId) {
        if (!StringUtils.hasText(invocationId)) {
            return null;
        }
        return apiInvocationMapper.selectRequestBodyJson(invocationId.trim());
    }

    @Override
    public long findRequestBodyByteSize(String invocationId) {
        if (!StringUtils.hasText(invocationId)) {
            return 0L;
        }
        Long size = apiInvocationMapper.selectRequestBodyByteSize(invocationId.trim());
        return size == null ? 0L : size;
    }
}
