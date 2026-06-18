package com.vtc.openapi.infra.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.botany.spore.ddd.infra.utils.convertor.ConvertHelper;
import com.vtc.openapi.domain.task.model.entity.OpenTaskScanResultDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskScanResultRepository;
import com.vtc.openapi.infra.dao.OpenTaskScanResultMapper;
import com.vtc.openapi.infra.dao.po.OpenTaskScanResultPO;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Repository
public class OpenTaskScanResultRepositoryImpl implements IOpenTaskScanResultRepository {

    private final OpenTaskScanResultMapper mapper;

    public OpenTaskScanResultRepositoryImpl(OpenTaskScanResultMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void upsertBatch(List<OpenTaskScanResultDO> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return;
        }
        for (OpenTaskScanResultDO row : rows) {
            if (row == null || !StringUtils.hasText(row.getSubId()) || !StringUtils.hasText(row.getResultKey())) {
                continue;
            }
            OpenTaskScanResultPO existing = mapper.selectOne(new LambdaQueryWrapper<OpenTaskScanResultPO>()
                    .eq(OpenTaskScanResultPO::getSubId, row.getSubId())
                    .eq(OpenTaskScanResultPO::getResultType, row.getResultType())
                    .eq(OpenTaskScanResultPO::getResultKey, row.getResultKey()));
            OpenTaskScanResultPO po = ConvertHelper.convert(row, OpenTaskScanResultPO.class);
            if (existing != null) {
                po.setId(existing.getId());
                mapper.updateById(po);
            } else {
                mapper.insert(po);
                row.setId(po.getId());
            }
        }
    }

    @Override
    public List<OpenTaskScanResultDO> listByTaskAndType(String taskId, int scanPhase, String resultType) {
        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(resultType)) {
            return Collections.emptyList();
        }
        List<OpenTaskScanResultPO> rows = mapper.selectList(new LambdaQueryWrapper<OpenTaskScanResultPO>()
                .eq(OpenTaskScanResultPO::getTaskId, taskId)
                .eq(OpenTaskScanResultPO::getScanPhase, scanPhase)
                .eq(OpenTaskScanResultPO::getResultType, resultType)
                .orderByAsc(OpenTaskScanResultPO::getId));
        return ConvertHelper.convertList(rows, OpenTaskScanResultDO.class);
    }

    @Override
    public List<OpenTaskScanResultDO> listBySubId(String subId, String resultType) {
        if (!StringUtils.hasText(subId)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<OpenTaskScanResultPO> wrapper = new LambdaQueryWrapper<OpenTaskScanResultPO>()
                .eq(OpenTaskScanResultPO::getSubId, subId)
                .orderByAsc(OpenTaskScanResultPO::getId);
        if (StringUtils.hasText(resultType)) {
            wrapper.eq(OpenTaskScanResultPO::getResultType, resultType);
        }
        return ConvertHelper.convertList(mapper.selectList(wrapper), OpenTaskScanResultDO.class);
    }

    @Override
    public int deleteBySubId(String subId) {
        if (!StringUtils.hasText(subId)) {
            return 0;
        }
        return mapper.delete(new LambdaQueryWrapper<OpenTaskScanResultPO>()
                .eq(OpenTaskScanResultPO::getSubId, subId.trim()));
    }
}
