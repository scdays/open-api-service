package com.vtc.openapi.app.service.impl;

import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.infra.utils.convertor.ConvertHelper;
import com.vtc.openapi.app.service.IInvocationAdminAppService;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.open.model.entity.ApiInvocationDO;
import com.vtc.openapi.domain.open.model.query.InvocationAdminQuery;
import com.vtc.openapi.domain.open.model.entity.WebhookDeliveryLogDO;
import com.vtc.openapi.domain.open.model.query.WebhookDeliveryLogQuery;
import com.vtc.openapi.domain.open.model.result.InvocationDailyTrendStat;
import com.vtc.openapi.domain.open.model.result.InvocationErrorCodeStat;
import com.vtc.openapi.domain.open.model.result.PartnerInvocationStatsResult;
import com.vtc.openapi.domain.open.model.result.PartnerQuotaStatResult;
import com.vtc.openapi.domain.partner.model.entity.PartnerDO;
import com.vtc.openapi.domain.partner.service.business.IPartnerDomainService;
import com.vtc.openapi.domain.open.service.business.IInvocationDomainService;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.InvocationDTO;
import com.vtc.openapi.ui.dto.admin.InvocationPageDto;
import com.vtc.openapi.ui.dto.admin.PartnerInvocationStatsDto;
import com.vtc.openapi.ui.dto.admin.PartnerQuotaDTO;
import com.vtc.openapi.ui.dto.admin.PartnerQuotaPageDto;
import com.vtc.openapi.ui.dto.admin.WebhookDeliveryLogDTO;
import com.vtc.openapi.ui.dto.admin.WebhookDeliveryLogPageDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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

    public InvocationAdminAppServiceImpl(IInvocationDomainService invocationDomainService,
                                         IPartnerDomainService partnerDomainService) {
        this.invocationDomainService = invocationDomainService;
        this.partnerDomainService = partnerDomainService;
    }

    @Override
    public ApiResponse<InvocationPageDto> listInvocations(PageInfo<InvocationDTO> pageInfo,
                                                          String partnerId,
                                                          String operationId,
                                                          Integer responseCode,
                                                          String startedFrom,
                                                          String startedTo) {
        if (pageInfo.getCurrent() < 1 || pageInfo.getSize() < 1) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "page/size 无效");
        }

        InvocationAdminQuery query = new InvocationAdminQuery();
        query.setPartnerId(partnerId);
        query.setOperationId(operationId);
        query.setResponseCode(responseCode);
        query.setStartedFrom(parseDate(startedFrom));
        query.setStartedTo(parseDate(startedTo));
        query.setPage((int) pageInfo.getCurrent());
        query.setSize((int) pageInfo.getSize());

        PageInfo<ApiInvocationDO> resultPage = invocationDomainService.pageInvocations(query);
        InvocationPageDto dto = new InvocationPageDto();
        dto.setItems(ConvertHelper.convertList(resultPage.getRecords(), InvocationDTO.class));
        dto.setPage((int) resultPage.getCurrent());
        dto.setSize((int) resultPage.getSize());
        dto.setTotal(resultPage.getTotal());
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
                                                                        String status) {
        if (pageInfo.getCurrent() < 1 || pageInfo.getSize() < 1) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "page/size 无效");
        }

        WebhookDeliveryLogQuery query = new WebhookDeliveryLogQuery();
        query.setPartnerId(partnerId);
        query.setEventType(eventType);
        query.setStatus(status);
        query.setPage((int) pageInfo.getCurrent());
        query.setSize((int) pageInfo.getSize());

        PageInfo<WebhookDeliveryLogDO> resultPage = invocationDomainService.pageWebhookDeliveries(query);
        WebhookDeliveryLogPageDto dto = new WebhookDeliveryLogPageDto();
        dto.setItems(ConvertHelper.convertList(resultPage.getRecords(), WebhookDeliveryLogDTO.class));
        dto.setPage((int) resultPage.getCurrent());
        dto.setSize((int) resultPage.getSize());
        dto.setTotal(resultPage.getTotal());
        return ApiResponse.ok(dto);
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
}
