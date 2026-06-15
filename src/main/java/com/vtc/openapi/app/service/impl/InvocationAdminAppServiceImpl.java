package com.vtc.openapi.app.service.impl;

import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.infra.utils.convertor.ConvertHelper;
import com.vtc.openapi.app.convert.AdminGovernanceAppConvertor;
import com.vtc.openapi.app.service.IInvocationAdminAppService;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.open.model.entity.ApiInvocationDO;
import com.vtc.openapi.domain.open.model.entity.WebhookDeliveryLogDO;
import com.vtc.openapi.domain.open.model.query.InvocationAdminQuery;
import com.vtc.openapi.domain.open.model.query.WebhookDeliveryLogQuery;
import com.vtc.openapi.domain.open.model.result.InvocationDailyTrendStat;
import com.vtc.openapi.domain.open.model.result.InvocationErrorCodeStat;
import com.vtc.openapi.domain.open.model.result.PartnerInvocationStatsResult;
import com.vtc.openapi.domain.open.model.result.PartnerQuotaStatResult;
import com.vtc.openapi.domain.open.model.support.WebhookDeliverySupport;
import com.vtc.openapi.domain.open.repository.IApiInvocationRepository;
import com.vtc.openapi.domain.partner.model.entity.PartnerDO;
import com.vtc.openapi.domain.partner.service.business.IPartnerDomainService;
import com.vtc.openapi.domain.open.service.business.IInvocationDomainService;
import com.vtc.openapi.domain.webhook.service.business.IWebhookDomainService;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.InvocationDetailDTO;
import com.vtc.openapi.ui.dto.admin.InvocationDTO;
import com.vtc.openapi.ui.dto.admin.InvocationPageDto;
import com.vtc.openapi.ui.dto.admin.InvocationResponseBodyDTO;
import com.vtc.openapi.ui.dto.admin.PartnerInvocationStatsDto;
import com.vtc.openapi.ui.dto.admin.PartnerQuotaDTO;
import com.vtc.openapi.ui.dto.admin.PartnerQuotaPageDto;
import com.vtc.openapi.ui.dto.admin.WebhookDeliveryLogDetailDTO;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSON;
import com.vtc.openapi.ui.dto.admin.WebhookDeliveryLogDTO;
import com.vtc.openapi.ui.dto.admin.WebhookDeliveryLogPageDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

@Service
public class InvocationAdminAppServiceImpl implements IInvocationAdminAppService {

    private static final SimpleDateFormat ISO_UTC;
    private static final SimpleDateFormat SIMPLE_DATETIME;
    private static final SimpleDateFormat SIMPLE_DATE;

    static {
        ISO_UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        ISO_UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
        SIMPLE_DATETIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SIMPLE_DATE = new SimpleDateFormat("yyyy-MM-dd");
    }

    private final IInvocationDomainService invocationDomainService;
    private final IPartnerDomainService partnerDomainService;
    private final IWebhookDomainService webhookDomainService;
    private final IApiInvocationRepository apiInvocationRepository;
    private final AdminGovernanceAppConvertor adminGovernanceAppConvertor;

    public InvocationAdminAppServiceImpl(IInvocationDomainService invocationDomainService,
                                         IPartnerDomainService partnerDomainService,
                                         IWebhookDomainService webhookDomainService,
                                         IApiInvocationRepository apiInvocationRepository,
                                         AdminGovernanceAppConvertor adminGovernanceAppConvertor) {
        this.invocationDomainService = invocationDomainService;
        this.partnerDomainService = partnerDomainService;
        this.webhookDomainService = webhookDomainService;
        this.apiInvocationRepository = apiInvocationRepository;
        this.adminGovernanceAppConvertor = adminGovernanceAppConvertor;
    }

    @Override
    public ApiResponse<InvocationPageDto> listInvocations(PageInfo<InvocationDTO> pageInfo,
                                                          String partnerId,
                                                          String operationId,
                                                          String domain,
                                                          Integer responseCode,
                                                          String resourceType,
                                                          String resourceId,
                                                          String startedFrom,
                                                          String startedTo) {
        if (pageInfo.getCurrent() < 1 || pageInfo.getSize() < 1) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "page/size 无效");
        }

        InvocationAdminQuery query = new InvocationAdminQuery();
        query.setPartnerId(partnerId);
        query.setOperationId(operationId);
        query.setDomain(domain);
        query.setResponseCode(responseCode);
        query.setResourceType(resourceType);
        query.setResourceId(resourceId);
        query.setStartedFrom(parseDate(startedFrom));
        query.setStartedTo(parseDate(startedTo));
        query.setPage((int) pageInfo.getCurrent());
        query.setSize((int) pageInfo.getSize());

        PageInfo<ApiInvocationDO> resultPage = invocationDomainService.pageInvocations(query);
        InvocationPageDto dto = new InvocationPageDto();
        dto.setItems(enrichInvocationList(resultPage.getRecords()));
        dto.setPage((int) resultPage.getCurrent());
        dto.setSize((int) resultPage.getSize());
        dto.setTotal(resultPage.getTotal());
        return ApiResponse.ok(dto);
    }

    @Override
    public ApiResponse<InvocationDetailDTO> getInvocationDetail(String invocationId) {
        ApiInvocationDO invocation = invocationDomainService.requireInvocation(invocationId);
        List<WebhookDeliveryLogDO> relatedWebhooks = invocationDomainService.listRelatedWebhookDeliveries(invocation);
        return ApiResponse.ok(toDetailDto(invocation, relatedWebhooks));
    }

    @Override
    public ApiResponse<InvocationResponseBodyDTO> getInvocationResponseBody(String invocationId) {
        ApiInvocationDO invocation = invocationDomainService.requireInvocation(invocationId);
        String storedJson = apiInvocationRepository.findResponseBodyJson(invocation.getInvocationId());
        boolean stored = StringUtils.hasText(storedJson);
        String bodyFormatted = stored
                ? formatJson(storedJson)
                : buildFallbackResponseBodyPreview(invocation);

        InvocationResponseBodyDTO dto = new InvocationResponseBodyDTO();
        dto.setInvocationId(invocation.getInvocationId());
        dto.setRequestId(invocation.getRequestId());
        dto.setStored(stored);
        dto.setBodyFormatted(bodyFormatted);
        dto.setByteSize(stored
                ? apiInvocationRepository.findResponseBodyByteSize(invocation.getInvocationId())
                : (long) bodyFormatted.length());
        return ApiResponse.ok(dto);
    }

    @Override
    public ApiResponse<PartnerInvocationStatsDto> getPartnerStats(String partnerId) {
        PartnerInvocationStatsResult result = invocationDomainService.queryPartnerStats(partnerId);
        PartnerInvocationStatsDto dto = new PartnerInvocationStatsDto();
        dto.setPartnerId(result.getPartnerId());
        dto.setTodayTotal(result.getTodayTotal());
        dto.setTodaySuccess(result.getTodaySuccess());
        dto.setTodaySuccessRate(result.getTodaySuccessRate());
        dto.setTopErrorCodes(toTopErrors(result.getTopErrorCodes()));
        dto.setDailyTrend(toDailyTrend(result.getDailyTrend()));
        return ApiResponse.ok(dto);
    }

    @Override
    public ApiResponse<PartnerQuotaPageDto> listPartnerQuotas(PageInfo<PartnerQuotaDTO> pageInfo,
                                                              String partnerId,
                                                              String partnerName,
                                                              String status,
                                                              String startedFrom,
                                                              String startedTo) {
        if (pageInfo.getCurrent() < 1 || pageInfo.getSize() < 1) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "page/size 无效");
        }

        Date from = parseDate(startedFrom);
        Date to = parseDate(startedTo);

        PageInfo<PartnerDO> partnerPage = new PageInfo<>();
        partnerPage.setCurrent(pageInfo.getCurrent());
        partnerPage.setSize(pageInfo.getSize());
        PageInfo<PartnerDO> resultPage = partnerDomainService.pagePartners(partnerPage, partnerId, partnerName, status);

        List<PartnerQuotaDTO> items = resultPage.getRecords().stream().map(partner -> {
            PartnerQuotaStatResult stat = invocationDomainService.queryPartnerQuotaStats(partner.getPartnerId(), from, to);
            PartnerQuotaDTO dto = ConvertHelper.convert(partner, PartnerQuotaDTO.class);
            dto.setTotalInvocations(stat.getTotalInvocations());
            dto.setSuccessInvocations(stat.getSuccessInvocations());
            dto.setFailedInvocations(stat.getFailedInvocations());
            dto.setSuccessRate(stat.getSuccessRate());
            return dto;
        }).collect(Collectors.toList());

        PartnerQuotaPageDto dto = new PartnerQuotaPageDto();
        dto.setItems(items);
        dto.setPage((int) resultPage.getCurrent());
        dto.setSize((int) resultPage.getSize());
        dto.setTotal(resultPage.getTotal());
        return ApiResponse.ok(dto);
    }

    @Override
    public ApiResponse<WebhookDeliveryLogPageDto> listWebhookDeliveries(PageInfo<WebhookDeliveryLogDTO> pageInfo,
                                                                        String partnerId,
                                                                        String eventType,
                                                                        String status,
                                                                        String resourceType,
                                                                        String resourceId) {
        if (pageInfo.getCurrent() < 1 || pageInfo.getSize() < 1) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "page/size 无效");
        }

        WebhookDeliveryLogQuery query = new WebhookDeliveryLogQuery();
        query.setPartnerId(partnerId);
        query.setEventType(eventType);
        query.setStatus(status);
        query.setResourceType(resourceType);
        query.setResourceId(resourceId);
        query.setPage((int) pageInfo.getCurrent());
        query.setSize((int) pageInfo.getSize());

        PageInfo<WebhookDeliveryLogDO> resultPage = invocationDomainService.pageWebhookDeliveries(query);
        WebhookDeliveryLogPageDto dto = new WebhookDeliveryLogPageDto();
        dto.setItems(adminGovernanceAppConvertor.toWebhookDeliveryLogDtoList(resultPage.getRecords()));
        dto.setPage((int) resultPage.getCurrent());
        dto.setSize((int) resultPage.getSize());
        dto.setTotal(resultPage.getTotal());
        return ApiResponse.ok(dto);
    }

    @Override
    public ApiResponse<WebhookDeliveryLogDetailDTO> getWebhookDeliveryDetail(Long deliveryId) {
        WebhookDeliveryLogDO source = enrichDeliveryMetadata(webhookDomainService.requireDeliveryLog(deliveryId));
        List<WebhookDeliveryLogDO> history = webhookDomainService.listRelatedAttempts(source);
        WebhookDeliveryLogDetailDTO dto = adminGovernanceAppConvertor.toWebhookDeliveryLogDetailDto(source);
        dto.setPayloadJsonFormatted(formatJson(source.getPayloadJson()));
        dto.setRetryHistory(toRetryHistory(history));
        dto.setRelatedInvocations(toRelatedInvocations(source));
        return ApiResponse.ok(dto);
    }

    @Override
    public ApiResponse<WebhookDeliveryLogDTO> retryWebhookDelivery(Long deliveryId) {
        WebhookDeliveryLogDO result = webhookDomainService.retryDelivery(deliveryId);
        return ApiResponse.ok(adminGovernanceAppConvertor.toWebhookDeliveryLogDto(result));
    }

    private List<PartnerInvocationStatsDto.ErrorCodeStatDto> toTopErrors(List<InvocationErrorCodeStat> stats) {
        if (stats == null) {
            return null;
        }
        return stats.stream().map(stat -> {
            PartnerInvocationStatsDto.ErrorCodeStatDto dto = new PartnerInvocationStatsDto.ErrorCodeStatDto();
            dto.setResponseCode(stat.getResponseCode());
            dto.setCount(stat.getCount());
            return dto;
        }).collect(Collectors.toList());
    }

    private List<PartnerInvocationStatsDto.DailyTrendDto> toDailyTrend(List<InvocationDailyTrendStat> stats) {
        if (stats == null) {
            return null;
        }
        return stats.stream().map(stat -> {
            PartnerInvocationStatsDto.DailyTrendDto dto = new PartnerInvocationStatsDto.DailyTrendDto();
            dto.setDate(stat.getStatDate().toString());
            dto.setTotalCount(stat.getTotalCount());
            dto.setSuccessCount(stat.getSuccessCount());
            dto.setFailCount(stat.getTotalCount() - stat.getSuccessCount());
            dto.setSuccessRate(calcRate(stat.getSuccessCount(), stat.getTotalCount()));
            return dto;
        }).collect(Collectors.toList());
    }

    private double calcRate(long success, long total) {
        if (total <= 0L) {
            return 0D;
        }
        return ((double) success) / (double) total;
    }

    private Date parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        ParseException parseException = null;
        try {
            synchronized (ISO_UTC) {
                return ISO_UTC.parse(value);
            }
        } catch (ParseException ex) {
            parseException = ex;
        }
        try {
            synchronized (SIMPLE_DATETIME) {
                return SIMPLE_DATETIME.parse(value);
            }
        } catch (ParseException ex) {
            parseException = ex;
        }
        try {
            synchronized (SIMPLE_DATE) {
                return SIMPLE_DATE.parse(value);
            }
        } catch (ParseException ex) {
            parseException = ex;
        }
        throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR,
                "时间格式无效，支持 yyyy-MM-dd / yyyy-MM-dd HH:mm:ss / ISO8601 UTC", parseException);
    }

    private List<InvocationDTO> enrichInvocationList(List<ApiInvocationDO> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        return records.stream().map(this::toListDto).collect(Collectors.toList());
    }

    private InvocationDTO toListDto(ApiInvocationDO row) {
        return adminGovernanceAppConvertor.toInvocationDto(row);
    }

    private InvocationDetailDTO toDetailDto(ApiInvocationDO row, List<WebhookDeliveryLogDO> relatedWebhooks) {
        InvocationDetailDTO dto = adminGovernanceAppConvertor.toInvocationDetailDto(row);
        dto.setResponseMessage(resolveResponseMessage(row));
        dto.setRequestHeadersPreview(buildRequestHeadersPreview(row));
        dto.setRequestBodyPreview(buildRequestBodyPreview(row));
        dto.setResponseHeadersPreview(buildResponseHeadersPreview(row));
        long responseBodyByteSize = apiInvocationRepository.findResponseBodyByteSize(row.getInvocationId());
        dto.setResponseBodyByteSize(responseBodyByteSize);
        dto.setHasResponseBody(responseBodyByteSize > 0L || canBuildFallbackResponseBody(row));
        dto.setTimeline(buildTimeline(row, relatedWebhooks));
        dto.setRelatedWebhooks(toRelatedWebhooks(relatedWebhooks));
        return dto;
    }

    private String resolveResponseMessage(ApiInvocationDO row) {
        if (row.getResponseCode() != null && row.getResponseCode() == 0) {
            return "ok";
        }
        return StringUtils.hasText(row.getErrorMessage()) ? row.getErrorMessage() : "failed";
    }

    private String buildRequestHeadersPreview(ApiInvocationDO row) {
        StringBuilder sb = new StringBuilder();
        sb.append("Authorization: Bearer ***\n");
        sb.append("X-Partner-Id: ").append(nullToDash(row.getPartnerId())).append('\n');
        sb.append("Content-Type: application/json\n");
        sb.append("X-Request-Id: ").append(nullToDash(row.getRequestId()));
        return sb.toString();
    }

    private String buildRequestBodyPreview(ApiInvocationDO row) {
        StringBuilder json = new StringBuilder("{\n");
        json.append("  \"operationId\": \"").append(escapeJson(row.getOperationId())).append("\",\n");
        json.append("  \"httpMethod\": \"").append(escapeJson(row.getHttpMethod())).append("\",\n");
        json.append("  \"requestPath\": \"").append(escapeJson(row.getRequestPath())).append("\"");
        if (StringUtils.hasText(row.getResourceId())) {
            json.append(",\n  \"resourceId\": \"").append(escapeJson(row.getResourceId())).append("\"");
        }
        json.append("\n}");
        return json.toString();
    }

    private String buildResponseHeadersPreview(ApiInvocationDO row) {
        StringBuilder sb = new StringBuilder();
        sb.append("Content-Type: application/json\n");
        sb.append("X-Request-Id: ").append(nullToDash(row.getRequestId()));
        if (row.getHttpStatus() != null) {
            sb.append("\nHTTP-Status: ").append(row.getHttpStatus());
        }
        return sb.toString();
    }

    private boolean canBuildFallbackResponseBody(ApiInvocationDO row) {
        return row.getResponseCode() != null
                || StringUtils.hasText(row.getRequestId())
                || StringUtils.hasText(row.getResourceId());
    }

    private String buildFallbackResponseBodyPreview(ApiInvocationDO row) {
        JSONObject envelope = new JSONObject(true);
        envelope.put("code", row.getResponseCode() == null ? 0 : row.getResponseCode());
        envelope.put("message", resolveResponseMessage(row));
        envelope.put("requestId", row.getRequestId());
        envelope.put("data", buildFallbackResponseData(row));
        return JSON.toJSONString(envelope, true);
    }

    private Object buildFallbackResponseData(ApiInvocationDO row) {
        if (!StringUtils.hasText(row.getResourceType()) && !StringUtils.hasText(row.getResourceId())) {
            return null;
        }
        JSONObject data = new JSONObject(true);
        if (StringUtils.hasText(row.getResourceId())) {
            if ("TASK".equalsIgnoreCase(row.getResourceType())) {
                data.put("taskId", row.getResourceId());
            } else if ("INSTANCE".equalsIgnoreCase(row.getResourceType())) {
                data.put("vulInfoID", row.getResourceId());
            } else if ("EXPORT".equalsIgnoreCase(row.getResourceType())) {
                data.put("exportId", row.getResourceId());
            } else {
                data.put("resourceId", row.getResourceId());
            }
        }
        return data.isEmpty() ? null : data;
    }

    private List<InvocationDetailDTO.InvocationTimelineItemDto> buildTimeline(
            ApiInvocationDO row, List<WebhookDeliveryLogDO> relatedWebhooks) {
        List<InvocationDetailDTO.InvocationTimelineItemDto> timeline = new ArrayList<>();
        timeline.add(timelineItem(formatDateTime(row.getStartedAt()),
                "partner-gateway 鉴权通过，注入 X-Partner-Id", false));
        timeline.add(timelineItem(formatDateTime(row.getStartedAt()),
                "open-api InvocationPipeline · operationId=" + nullToDash(row.getOperationId()), false));
        if (StringUtils.hasText(row.getResourceId())) {
            timeline.add(timelineItem(formatDateTime(row.getFinishedAt() != null ? row.getFinishedAt() : row.getStartedAt()),
                    "资源 " + row.getResourceType() + " / " + row.getResourceId(), false));
        }
        boolean failed = row.getResponseCode() != null && row.getResponseCode() != 0;
        timeline.add(timelineItem(formatDateTime(row.getFinishedAt()),
                "写入 api_invocation · code=" + row.getResponseCode()
                        + (row.getLatencyMs() != null ? " · " + row.getLatencyMs() + "ms" : ""),
                failed));
        if (relatedWebhooks != null) {
            for (WebhookDeliveryLogDO webhook : relatedWebhooks) {
                timeline.add(timelineItem(formatDateTime(webhook.getCreatedAt()),
                        "Webhook " + webhook.getEventType() + " · HTTP "
                                + (webhook.getHttpStatus() == null ? "-" : webhook.getHttpStatus()),
                        webhook.getHttpStatus() != null && webhook.getHttpStatus() >= 400));
            }
        }
        return timeline;
    }

    private InvocationDetailDTO.InvocationTimelineItemDto timelineItem(String at, String message, boolean failed) {
        InvocationDetailDTO.InvocationTimelineItemDto item = new InvocationDetailDTO.InvocationTimelineItemDto();
        item.setOccurredAt(at);
        item.setMessage(message);
        item.setFailed(failed);
        return item;
    }

    private List<InvocationDetailDTO.InvocationRelatedWebhookDto> toRelatedWebhooks(
            List<WebhookDeliveryLogDO> relatedWebhooks) {
        if (relatedWebhooks == null || relatedWebhooks.isEmpty()) {
            return Collections.emptyList();
        }
        return relatedWebhooks.stream().map(webhook -> {
            InvocationDetailDTO.InvocationRelatedWebhookDto dto = new InvocationDetailDTO.InvocationRelatedWebhookDto();
            dto.setId(webhook.getId());
            dto.setEventId(webhook.getEventId());
            dto.setEventType(webhook.getEventType());
            dto.setResourceType(webhook.getResourceType());
            dto.setResourceId(webhook.getResourceId());
            dto.setStatus(webhook.getStatus());
            dto.setHttpStatus(webhook.getHttpStatus());
            dto.setCreatedAt(formatDateTime(webhook.getCreatedAt()));
            return dto;
        }).collect(Collectors.toList());
    }

    private String formatDateTime(Date value) {
        if (value == null) {
            return "-";
        }
        synchronized (SIMPLE_DATETIME) {
            return SIMPLE_DATETIME.format(value);
        }
    }

    private String nullToDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String formatJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        try {
            Object parsed = com.alibaba.fastjson.JSON.parse(raw);
            return com.alibaba.fastjson.JSON.toJSONString(parsed, true);
        } catch (Exception ex) {
            return raw;
        }
    }

    private List<WebhookDeliveryLogDetailDTO.WebhookDeliveryRetryHistoryDto> toRetryHistory(
            List<WebhookDeliveryLogDO> history) {
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }
        return history.stream().map(row -> {
            WebhookDeliveryLogDetailDTO.WebhookDeliveryRetryHistoryDto item =
                    new WebhookDeliveryLogDetailDTO.WebhookDeliveryRetryHistoryDto();
            item.setId(row.getId());
            item.setRetryCount(row.getRetryCount());
            item.setHttpStatus(row.getHttpStatus());
            item.setStatus(row.getStatus());
            item.setCreatedAt(formatDateTime(row.getCreatedAt()));
            item.setTriggerSource(row.getTriggerSource());
            item.setTriggerSourceLabel(triggerSourceLabel(row.getTriggerSource()));
            item.setManualRetry(WebhookDeliverySupport.TRIGGER_MANUAL_RETRY.equals(row.getTriggerSource()));
            return item;
        }).collect(Collectors.toList());
    }

    private WebhookDeliveryLogDO enrichDeliveryMetadata(WebhookDeliveryLogDO source) {
        if (source == null) {
            return null;
        }
        if (!StringUtils.hasText(source.getEventId())) {
            source.setEventId(WebhookDeliverySupport.parseEventId(source.getPayloadJson()));
        }
        if (!StringUtils.hasText(source.getResourceId())) {
            WebhookDeliverySupport.ResourceBinding binding =
                    WebhookDeliverySupport.extractResource(source.getEventType(), source.getPayloadJson());
            if (binding != null) {
                source.setResourceType(binding.getResourceType());
                source.setResourceId(binding.getResourceId());
                source.setResourceIdsJson(binding.getResourceIdsJson());
            }
        }
        return source;
    }

    private List<WebhookDeliveryLogDetailDTO.RelatedInvocationDto> toRelatedInvocations(WebhookDeliveryLogDO source) {
        WebhookDeliveryLogDO enriched = enrichDeliveryMetadata(source);
        if (enriched == null || !StringUtils.hasText(enriched.getPartnerId())) {
            return Collections.emptyList();
        }
        String linkResourceId = WebhookDeliverySupport.resolveInvocationLinkResourceId(
                enriched.getEventType(), enriched.getResourceId(), enriched.getPayloadJson());
        if (!StringUtils.hasText(linkResourceId)) {
            return Collections.emptyList();
        }
        List<ApiInvocationDO> rows = apiInvocationRepository.listInvocationsByResource(
                enriched.getPartnerId(), null, linkResourceId, 10);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream().map(row -> {
            WebhookDeliveryLogDetailDTO.RelatedInvocationDto dto =
                    new WebhookDeliveryLogDetailDTO.RelatedInvocationDto();
            dto.setInvocationId(row.getInvocationId());
            dto.setOperationId(row.getOperationId());
            dto.setResourceType(row.getResourceType());
            dto.setResourceId(row.getResourceId());
            dto.setResponseCode(row.getResponseCode());
            dto.setStartedAt(formatDateTime(row.getStartedAt()));
            return dto;
        }).collect(Collectors.toList());
    }

    private String triggerSourceLabel(String triggerSource) {
        if (WebhookDeliverySupport.TRIGGER_FIRST_ATTEMPT.equals(triggerSource)) {
            return "首次投递";
        }
        if (WebhookDeliverySupport.TRIGGER_AUTO_RETRY.equals(triggerSource)) {
            return "自动重试";
        }
        if (WebhookDeliverySupport.TRIGGER_MANUAL_RETRY.equals(triggerSource)) {
            return "手动重试";
        }
        return triggerSource == null ? "-" : triggerSource;
    }
}
