package com.vtc.openapi.infra.repository;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.botany.spore.ddd.infra.repository.DatabaseRepositoryImpl;
import com.botany.spore.ddd.infra.utils.convertor.ConvertHelper;
import com.vtc.openapi.domain.instance.model.command.SearchInstanceCommand;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.instance.model.result.InstanceItemResult;
import com.vtc.openapi.domain.instance.model.result.InstancePageResult;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.infra.converter.InstanceItemConverter;
import com.vtc.openapi.infra.dao.OpenVulnInstanceMapper;
import com.vtc.openapi.infra.dao.po.OpenVulnInstancePO;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Repository
public class OpenVulnInstanceRepositoryImpl
        extends DatabaseRepositoryImpl<OpenVulnInstanceMapper, OpenVulnInstanceDO, OpenVulnInstancePO>
        implements IOpenVulnInstanceRepository {

    @Override
    public boolean existsByPartnerAndTaskId(String partnerId, String taskId) {
        return countByPartnerAndTaskId(partnerId, taskId) > 0;
    }

    @Override
    public long countByPartnerAndTaskId(String partnerId, String taskId) {
        if (!StringUtils.hasText(partnerId) || !StringUtils.hasText(taskId)) {
            return 0;
        }
        return baseMapper.selectCount(new LambdaQueryWrapper<OpenVulnInstancePO>()
                .eq(OpenVulnInstancePO::getPartnerId, partnerId)
                .eq(OpenVulnInstancePO::getTaskId, taskId));
    }

    @Override
    public List<OpenVulnInstanceDO> listByPartnerAndTask(String partnerId, String taskId, String extTaskId) {
        LambdaQueryWrapper<OpenVulnInstancePO> wrapper = new LambdaQueryWrapper<OpenVulnInstancePO>()
                .eq(OpenVulnInstancePO::getPartnerId, partnerId);
        if (StringUtils.hasText(taskId)) {
            wrapper.eq(OpenVulnInstancePO::getTaskId, taskId);
        } else if (StringUtils.hasText(extTaskId)) {
            wrapper.eq(OpenVulnInstancePO::getExtTaskId, extTaskId);
        } else {
            return new ArrayList<>();
        }
        return ConvertHelper.convertList(baseMapper.selectList(wrapper), OpenVulnInstanceDO.class);
    }

    @Override
    public OpenVulnInstanceDO findByPartnerAndVulInfoId(String partnerId, String vulInfoId) {
        if (!StringUtils.hasText(partnerId) || !StringUtils.hasText(vulInfoId)) {
            return null;
        }
        OpenVulnInstancePO po = baseMapper.selectOne(new LambdaQueryWrapper<OpenVulnInstancePO>()
                .eq(OpenVulnInstancePO::getPartnerId, partnerId)
                .eq(OpenVulnInstancePO::getVulInfoId, vulInfoId));
        return ConvertHelper.convert(po, OpenVulnInstanceDO.class);
    }

    @Override
    public OpenVulnInstanceDO findByIdAndPartner(Long id, String partnerId) {
        if (id == null || !StringUtils.hasText(partnerId)) {
            return null;
        }
        OpenVulnInstancePO po = baseMapper.selectOne(new LambdaQueryWrapper<OpenVulnInstancePO>()
                .eq(OpenVulnInstancePO::getId, id)
                .eq(OpenVulnInstancePO::getPartnerId, partnerId));
        return ConvertHelper.convert(po, OpenVulnInstanceDO.class);
    }

    @Override
    public InstancePageResult searchFromDb(String partnerId, SearchInstanceCommand command) {
        List<OpenVulnInstanceDO> rows = listByPartnerAndTask(
                partnerId, command.getTaskId(), command.getExtTaskId());
        List<InstanceItemResult> filtered = new ArrayList<>();
        for (OpenVulnInstanceDO row : rows) {
            InstanceItemResult item = InstanceItemConverter.fromSnapshot(row);
            if (matchesFilters(item, command)) {
                filtered.add(item);
            }
        }
        int page = Math.max(1, command.getPage() != null ? command.getPage() : 1);
        int size = Math.min(1000, Math.max(1, command.getSize() != null ? command.getSize() : 20));
        int from = (page - 1) * size;
        int to = Math.min(from + size, filtered.size());
        InstancePageResult result = new InstancePageResult();
        result.setTotal((long) filtered.size());
        result.setPage(page);
        result.setSize(size);
        result.setItems(from < filtered.size()
                ? new ArrayList<>(filtered.subList(from, to)) : new ArrayList<>());
        return result;
    }

    @Override
    public void batchInsert(List<OpenVulnInstanceDO> instances) {
        if (CollectionUtils.isEmpty(instances)) {
            return;
        }
        for (OpenVulnInstanceDO row : instances) {
            baseMapper.insert(ConvertHelper.convert(row, OpenVulnInstancePO.class));
        }
    }

    @Override
    public void updateState(Long id, String partnerId, int vulInfoStat, String method, String remedDesc) {
        OpenVulnInstanceDO row = findByIdAndPartner(id, partnerId);
        if (row == null) {
            return;
        }
        row.setVulInfoStat(vulInfoStat);
        row.setUpdatedAt(new Date());
        if (StringUtils.hasText(row.getSnapshotJson())) {
            JSONObject snap = JSON.parseObject(row.getSnapshotJson());
            snap.put("vulInfoStat", vulInfoStat);
            if (StringUtils.hasText(method)) {
                snap.put("method", method);
                snap.put("srcMethod", method);
            }
            if (StringUtils.hasText(remedDesc)) {
                snap.put("remedDesc", remedDesc);
            }
            row.setSnapshotJson(snap.toJSONString());
        }
        baseMapper.updateById(ConvertHelper.convert(row, OpenVulnInstancePO.class));
    }

    private boolean matchesFilters(InstanceItemResult item, SearchInstanceCommand command) {
        if (command.getVulInfoStatList() != null && !command.getVulInfoStatList().isEmpty()) {
            if (item.getVulInfoStat() == null
                    || !command.getVulInfoStatList().contains(item.getVulInfoStat())) {
                return false;
            }
        }
        if (command.getVulLevelList() != null && !command.getVulLevelList().isEmpty()) {
            if (item.getVulLevel() == null) {
                return false;
            }
            boolean levelMatch = false;
            for (String level : command.getVulLevelList()) {
                if (Objects.equals(String.valueOf(item.getVulLevel()), level)) {
                    levelMatch = true;
                    break;
                }
            }
            if (!levelMatch) {
                return false;
            }
        }
        if (StringUtils.hasText(command.getVulNetAddr())
                && !Objects.equals(command.getVulNetAddr(), item.getVulNetAddr())) {
            return false;
        }
        if (StringUtils.hasText(command.getAssetName())
                && !Objects.equals(command.getAssetName(), item.getAssetName())) {
            return false;
        }
        if (StringUtils.hasText(command.getVulName())
                && !Objects.equals(command.getVulName(), item.getVulName())) {
            return false;
        }
        if (StringUtils.hasText(command.getOrgVulId())
                && !Objects.equals(command.getOrgVulId(), item.getOrgVulId())) {
            return false;
        }
        if (StringUtils.hasText(command.getVulId())
                && !Objects.equals(command.getVulId(), item.getVulId())) {
            return false;
        }
        if (command.getIsAccess() != null) {
            int expected = command.getIsAccess() ? 1 : 0;
            if (item.getIsAccess() == null || item.getIsAccess() != expected) {
                return false;
            }
        }
        if (StringUtils.hasText(command.getUnitType())
                && !Objects.equals(command.getUnitType(), item.getUnitType())) {
            return false;
        }
        return true;
    }
}
