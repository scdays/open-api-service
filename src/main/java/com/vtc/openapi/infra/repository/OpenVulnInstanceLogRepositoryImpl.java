package com.vtc.openapi.infra.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.botany.spore.ddd.infra.utils.convertor.ConvertHelper;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceLogDO;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceLogRepository;
import com.vtc.openapi.infra.dao.OpenVulnInstanceLogMapper;
import com.vtc.openapi.infra.dao.po.OpenVulnInstanceLogPO;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Repository
public class OpenVulnInstanceLogRepositoryImpl implements IOpenVulnInstanceLogRepository {

    private final OpenVulnInstanceLogMapper mapper;

    public OpenVulnInstanceLogRepositoryImpl(OpenVulnInstanceLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void insertBatch(List<OpenVulnInstanceLogDO> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return;
        }
        for (OpenVulnInstanceLogDO row : rows) {
            mapper.insert(ConvertHelper.convert(row, OpenVulnInstanceLogPO.class));
        }
    }

    @Override
    public List<OpenVulnInstanceLogDO> listByVulInfoId(String partnerId, String vulInfoId, int limit) {
        if (!StringUtils.hasText(partnerId) || !StringUtils.hasText(vulInfoId)) {
            return new ArrayList<>();
        }
        int capped = Math.max(1, Math.min(limit, 500));
        List<OpenVulnInstanceLogPO> rows = mapper.selectList(new LambdaQueryWrapper<OpenVulnInstanceLogPO>()
                .eq(OpenVulnInstanceLogPO::getPartnerId, partnerId.trim())
                .eq(OpenVulnInstanceLogPO::getVulInfoId, vulInfoId.trim())
                .orderByDesc(OpenVulnInstanceLogPO::getId)
                .last("LIMIT " + capped));
        return ConvertHelper.convertList(rows, OpenVulnInstanceLogDO.class);
    }
}
