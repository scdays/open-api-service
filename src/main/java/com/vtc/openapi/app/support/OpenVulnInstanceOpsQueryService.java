package com.vtc.openapi.app.support;

import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobItemDO;
import com.vtc.openapi.domain.instance.model.result.InstanceItemResult;
import com.vtc.openapi.domain.instance.repository.IOpenVerifyFixJobRepository;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.infra.converter.InstanceItemConverter;
import com.vtc.openapi.ui.dto.admin.MockVulnInstanceOpsRowDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 运营页查询 open_vuln_instance（mock / task-center 通用）。
 */
@Service
public class OpenVulnInstanceOpsQueryService {

    private final IOpenVulnInstanceRepository vulnInstanceRepository;
    private final IOpenVerifyFixJobRepository verifyFixJobRepository;

    public OpenVulnInstanceOpsQueryService(IOpenVulnInstanceRepository vulnInstanceRepository,
                                           IOpenVerifyFixJobRepository verifyFixJobRepository) {
        this.vulnInstanceRepository = vulnInstanceRepository;
        this.verifyFixJobRepository = verifyFixJobRepository;
    }

    public List<MockVulnInstanceOpsRowDto> listForOps(String partnerId, String taskId,
                                                      Integer vulInfoStat, int limit) {
        if (!StringUtils.hasText(partnerId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "partnerId 不能为空");
        }
        int capped = Math.max(1, Math.min(limit, 500));
        String normalizedTaskId = StringUtils.hasText(taskId) ? taskId.trim() : null;
        List<OpenVulnInstanceDO> rows = vulnInstanceRepository.listByPartner(
                partnerId.trim(), normalizedTaskId, vulInfoStat, capped);
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
            OpenVerifyFixJobItemDO pending = null;
            if (StringUtils.hasText(row.getVulInfoId())) {
                pending = verifyFixJobRepository
                        .findLatestPendingItemByPartnerAndVulInfoId(partnerId.trim(), row.getVulInfoId());
            }
            if (pending != null && StringUtils.hasText(pending.getJobId())) {
                dto.setPendingVerifyFixJobId(pending.getJobId());
            }
            result.add(dto);
        }
        return result;
    }
}
