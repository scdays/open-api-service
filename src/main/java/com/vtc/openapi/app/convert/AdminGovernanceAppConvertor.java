package com.vtc.openapi.app.convert;

import com.vtc.openapi.domain.open.model.entity.ApiInvocationDO;
import com.vtc.openapi.domain.open.model.entity.WebhookDeliveryLogDO;
import com.vtc.openapi.domain.open.model.support.InvocationDomainSupport;
import com.vtc.openapi.domain.open.model.support.WebhookDeliverySupport;
import com.vtc.openapi.domain.open.model.support.WebhookDeliverySupport.ExportReadyInfo;
import com.vtc.openapi.domain.open.model.support.WebhookDeliverySupport.ResourceBinding;
import com.vtc.openapi.domain.webhook.model.WebhookEventType;
import com.vtc.openapi.ui.dto.admin.InvocationDTO;
import com.vtc.openapi.ui.dto.admin.InvocationDetailDTO;
import com.vtc.openapi.ui.dto.admin.WebhookDeliveryLogDTO;
import com.vtc.openapi.ui.dto.admin.WebhookDeliveryLogDetailDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin governance DTO mapping. Do not use ConvertHelper for *DetailDTO subclasses.
 */
@Component
public class AdminGovernanceAppConvertor {

    public InvocationDTO toInvocationDto(ApiInvocationDO row) {
        if (row == null) {
            return null;
        }
        InvocationDTO dto = new InvocationDTO();
        copyInvocation(row, dto);
        dto.setDomain(InvocationDomainSupport.resolveDomain(row.getOperationId(), row.getResourceType()));
        return dto;
    }

    public InvocationDetailDTO toInvocationDetailDto(ApiInvocationDO row) {
        if (row == null) {
            return null;
        }
        InvocationDetailDTO dto = new InvocationDetailDTO();
        copyInvocation(row, dto);
        dto.setDomain(InvocationDomainSupport.resolveDomain(row.getOperationId(), row.getResourceType()));
        dto.setClientIp(row.getClientIp());
        return dto;
    }

    public List<InvocationDTO> toInvocationDtoList(List<ApiInvocationDO> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyList();
        }
        return rows.stream().map(this::toInvocationDto).collect(Collectors.toList());
    }

    public WebhookDeliveryLogDTO toWebhookDeliveryLogDto(WebhookDeliveryLogDO row) {
        if (row == null) {
            return null;
        }
        WebhookDeliveryLogDTO dto = new WebhookDeliveryLogDTO();
        copyWebhookDeliveryLog(row, dto);
        return dto;
    }

    public WebhookDeliveryLogDetailDTO toWebhookDeliveryLogDetailDto(WebhookDeliveryLogDO row) {
        if (row == null) {
            return null;
        }
        WebhookDeliveryLogDetailDTO dto = new WebhookDeliveryLogDetailDTO();
        copyWebhookDeliveryLog(row, dto);
        dto.setPayloadJson(row.getPayloadJson());
        return dto;
    }

    public List<WebhookDeliveryLogDTO> toWebhookDeliveryLogDtoList(List<WebhookDeliveryLogDO> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyList();
        }
        return rows.stream().map(this::toWebhookDeliveryLogDto).collect(Collectors.toList());
    }

    private void copyInvocation(ApiInvocationDO row, InvocationDTO dto) {
        dto.setInvocationId(row.getInvocationId());
        dto.setRequestId(row.getRequestId());
        dto.setPartnerId(row.getPartnerId());
        dto.setOperationId(row.getOperationId());
        dto.setHttpMethod(row.getHttpMethod());
        dto.setResponseCode(row.getResponseCode());
        dto.setHttpStatus(row.getHttpStatus());
        dto.setLatencyMs(row.getLatencyMs());
        dto.setRequestPath(row.getRequestPath());
        dto.setResourceType(row.getResourceType());
        dto.setResourceId(row.getResourceId());
        dto.setErrorMessage(row.getErrorMessage());
        dto.setStartedAt(row.getStartedAt());
        dto.setFinishedAt(row.getFinishedAt());
    }

    private void copyWebhookDeliveryLog(WebhookDeliveryLogDO row, WebhookDeliveryLogDTO dto) {
        dto.setId(row.getId());
        dto.setPartnerId(row.getPartnerId());
        dto.setEventType(row.getEventType());
        dto.setEventId(row.getEventId());
        dto.setResourceType(row.getResourceType());
        dto.setResourceId(row.getResourceId());
        enrichWebhookLinkFields(row, dto);
        enrichExportReadyFields(row, dto);
        dto.setCallbackUrl(row.getCallbackUrl());
        dto.setHttpStatus(row.getHttpStatus());
        dto.setRetryCount(row.getRetryCount());
        dto.setStatus(row.getStatus());
        dto.setCreatedAt(row.getCreatedAt());
        dto.setNextRetryAt(row.getNextRetryAt());
    }

    private void enrichWebhookLinkFields(WebhookDeliveryLogDO row, WebhookDeliveryLogDTO dto) {
        if (row == null || dto == null) {
            return;
        }
        ResourceBinding binding = WebhookDeliverySupport.extractResource(row.getEventType(), row.getPayloadJson());
        if (binding == null) {
            return;
        }
        if (!org.springframework.util.StringUtils.hasText(dto.getResourceId())
                && org.springframework.util.StringUtils.hasText(binding.getResourceId())) {
            dto.setResourceId(binding.getResourceId());
        }
        if (!org.springframework.util.StringUtils.hasText(dto.getResourceType())
                && org.springframework.util.StringUtils.hasText(binding.getResourceType())) {
            dto.setResourceType(binding.getResourceType());
        }
        if (org.springframework.util.StringUtils.hasText(binding.getSecondaryResourceId())) {
            dto.setRelatedTaskId(binding.getSecondaryResourceId());
        }
    }

    private void enrichExportReadyFields(WebhookDeliveryLogDO row, WebhookDeliveryLogDTO dto) {
        if (row == null || dto == null || !WebhookEventType.EXPORT_READY.equals(row.getEventType())) {
            return;
        }
        ExportReadyInfo exportReady = WebhookDeliverySupport.extractExportReady(row.getEventType(), row.getPayloadJson());
        if (exportReady == null) {
            dto.setExportDownloadable(false);
            return;
        }
        dto.setExportId(exportReady.getExportId());
        dto.setExportFormat(exportReady.getFormat());
        dto.setExportStage(exportReady.getExportStage());
        dto.setPartnerDownloadUrl(exportReady.getDownloadUrl());
        if (!org.springframework.util.StringUtils.hasText(dto.getRelatedTaskId())) {
            dto.setRelatedTaskId(exportReady.getTaskId());
        }
        dto.setExportDownloadable("SUCCESS".equalsIgnoreCase(row.getStatus())
                && org.springframework.util.StringUtils.hasText(exportReady.getExportId()));
    }
}
