package com.vtc.openapi.app.service.impl;

import com.vtc.openapi.app.service.IOpenVulnInstanceAdminAppService;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceLogDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobItemDO;
import com.vtc.openapi.domain.instance.model.result.InstanceItemResult;
import com.vtc.openapi.domain.instance.repository.IOpenVerifyFixJobRepository;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceLogRepository;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.infra.converter.InstanceItemConverter;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.MockVulnInstanceOpsRowDto;
import com.vtc.openapi.ui.dto.admin.OpenVulnInstanceStateLogDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class OpenVulnInstanceAdminAppServiceImpl implements IOpenVulnInstanceAdminAppService {

    private static final DateTimeFormatter ISO_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private final IOpenVulnInstanceLogRepository logRepository;
    private final IOpenVulnInstanceRepository vulnInstanceRepository;
    private final IOpenVerifyFixJobRepository verifyFixJobRepository;

    public OpenVulnInstanceAdminAppServiceImpl(IOpenVulnInstanceLogRepository logRepository,
                                               IOpenVulnInstanceRepository vulnInstanceRepository,
                                               IOpenVerifyFixJobRepository verifyFixJobRepository) {
        this.logRepository = logRepository;
        this.vulnInstanceRepository = vulnInstanceRepository;
        this.verifyFixJobRepository = verifyFixJobRepository;
    }

    @Override
    public ApiResponse<List<OpenVulnInstanceStateLogDto>> listStateLogs(String partnerId, String vulInfoId, int limit) {
        if (!StringUtils.hasText(partnerId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "partnerId 不能为空");
        }
        if (!StringUtils.hasText(vulInfoId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "vulInfoId 不能为空");
        }
        if (limit < 1 || limit > 500) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "limit 须在 1-500");
        }
        List<OpenVulnInstanceLogDO> rows = logRepository.listByVulInfoId(partnerId.trim(), vulInfoId.trim(), limit);
        List<OpenVulnInstanceStateLogDto> list = new ArrayList<>();
        for (OpenVulnInstanceLogDO row : rows) {
            list.add(toDto(row));
        }
        return ApiResponse.ok(list);
    }

    @Override
    public ApiResponse<List<MockVulnInstanceOpsRowDto>> listInstancesForOps(String partnerId, String taskId,
                                                                           Integer vulInfoStat, int limit) {
        if (!StringUtils.hasText(partnerId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "partnerId 不能为空");
        }
        int capped = Math.max(1, Math.min(limit, 500));
        String normalizedTaskId = StringUtils.hasText(taskId) ? taskId.trim() : null;
        String normalizedPartnerId = partnerId.trim();
        List<OpenVulnInstanceDO> rows = vulnInstanceRepository.listByPartner(
                normalizedPartnerId, normalizedTaskId, vulInfoStat, capped);
        List<MockVulnInstanceOpsRowDto> result = new ArrayList<>();
        for (OpenVulnInstanceDO row : rows) {
            if (row == null) {
                continue;
            }
            MockVulnInstanceOpsRowDto dto = new MockVulnInstanceOpsRowDto();
            dto.setVulInfoId(row.getVulInfoId());
            dto.setVulInfoStat(row.getVulInfoStat());
            dto.setTaskId(row.getTaskId());
            InstanceItemResult item = InstanceItemConverter.fromSnapshot(row);
            if (item != null) {
                dto.setVulName(item.getVulName());
                dto.setVulNetAddr(item.getVulNetAddr());
                dto.setVulPort(item.getVulPort());
            }
            if (StringUtils.hasText(row.getVulInfoId())) {
                OpenVerifyFixJobItemDO pending = verifyFixJobRepository
                        .findLatestPendingItemByPartnerAndVulInfoId(normalizedPartnerId, row.getVulInfoId());
                if (pending != null && StringUtils.hasText(pending.getJobId())) {
                    dto.setPendingVerifyFixJobId(pending.getJobId());
                }
            }
            result.add(dto);
        }
        return ApiResponse.ok(result);
    }

    private static OpenVulnInstanceStateLogDto toDto(OpenVulnInstanceLogDO row) {
        OpenVulnInstanceStateLogDto dto = new OpenVulnInstanceStateLogDto();
        dto.setId(row.getId());
        dto.setVulInfoId(row.getVulInfoId());
        dto.setTaskId(row.getTaskId());
        dto.setSubId(row.getSubId());
        dto.setScanPhase(row.getScanPhase());
        dto.setPrevStat(row.getPrevStat());
        dto.setVulInfoStat(row.getVulInfoStat());
        dto.setChangeReason(row.getChangeReason());
        dto.setVerifyMergeStrategy(row.getVerifyMergeStrategy());
        dto.setScannerHitCount(row.getScannerHitCount());
        dto.setTransferTime(row.getTransferTime());
        dto.setCaseId(row.getCaseId());
        if (row.getCreatedAt() != null) {
            dto.setCreatedAt(ISO_UTC.format(row.getCreatedAt().toInstant()));
        }
        return dto;
    }
}
