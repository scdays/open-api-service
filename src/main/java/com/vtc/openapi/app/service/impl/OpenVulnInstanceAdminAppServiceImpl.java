package com.vtc.openapi.app.service.impl;

import com.vtc.openapi.app.service.IOpenVulnInstanceAdminAppService;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceLogDO;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceLogRepository;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.OpenVulnInstanceStateLogDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

@Service
public class OpenVulnInstanceAdminAppServiceImpl implements IOpenVulnInstanceAdminAppService {

    private static final SimpleDateFormat ISO_UTC;

    static {
        ISO_UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        ISO_UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final IOpenVulnInstanceLogRepository logRepository;

    public OpenVulnInstanceAdminAppServiceImpl(IOpenVulnInstanceLogRepository logRepository) {
        this.logRepository = logRepository;
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
            dto.setCreatedAt(ISO_UTC.format(row.getCreatedAt()));
        }
        return dto;
    }
}
