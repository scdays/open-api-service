package com.vtc.openapi.infra.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.botany.spore.ddd.infra.repository.DatabaseRepositoryImpl;
import com.botany.spore.ddd.infra.utils.convertor.ConvertHelper;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskSubRepository;
import com.vtc.openapi.infra.dao.OpenTaskSubMapper;
import com.vtc.openapi.infra.dao.po.OpenTaskSubPO;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
public class OpenTaskSubRepositoryImpl
        extends DatabaseRepositoryImpl<OpenTaskSubMapper, OpenTaskSubDO, OpenTaskSubPO>
        implements IOpenTaskSubRepository {

    @Override
    public List<OpenTaskSubDO> listByTaskId(String taskId) {
        List<OpenTaskSubPO> rows = baseMapper.selectList(new LambdaQueryWrapper<OpenTaskSubPO>()
                .eq(OpenTaskSubPO::getTaskId, taskId)
                .orderByAsc(OpenTaskSubPO::getId));
        return ConvertHelper.convertList(rows, OpenTaskSubDO.class);
    }

    @Override
    public List<OpenTaskSubDO> listByTaskIdAndPhase(String taskId, int scanPhase) {
        List<OpenTaskSubPO> rows = baseMapper.selectList(new LambdaQueryWrapper<OpenTaskSubPO>()
                .eq(OpenTaskSubPO::getTaskId, taskId)
                .eq(OpenTaskSubPO::getScanPhase, scanPhase)
                .orderByAsc(OpenTaskSubPO::getId));
        return ConvertHelper.convertList(rows, OpenTaskSubDO.class);
    }

    @Override
    public List<OpenTaskSubDO> listRunning() {
        List<OpenTaskSubPO> rows = baseMapper.selectList(new LambdaQueryWrapper<OpenTaskSubPO>()
                .in(OpenTaskSubPO::getStatus, "PENDING", "RUNNING")
                .orderByAsc(OpenTaskSubPO::getId)
                .last("LIMIT 200"));
        return ConvertHelper.convertList(rows, OpenTaskSubDO.class);
    }

    @Override
    public OpenTaskSubDO findBySubId(String subId) {
        OpenTaskSubPO po = baseMapper.selectOne(new LambdaQueryWrapper<OpenTaskSubPO>()
                .eq(OpenTaskSubPO::getSubId, subId));
        return ConvertHelper.convert(po, OpenTaskSubDO.class);
    }

    @Override
    public void saveSub(OpenTaskSubDO row) {
        OpenTaskSubPO po = ConvertHelper.convert(row, OpenTaskSubPO.class);
        baseMapper.insert(po);
        row.setId(po.getId());
    }

    @Override
    public void updateSub(OpenTaskSubDO row) {
        if (row == null || row.getId() == null) {
            return;
        }
        OpenTaskSubPO po = ConvertHelper.convert(row, OpenTaskSubPO.class);
        baseMapper.updateById(po);
        // updateById 默认忽略 null 字段，下发成功后需显式清空 error_message
        if (!StringUtils.hasText(row.getErrorMessage())) {
            baseMapper.update(null, new LambdaUpdateWrapper<OpenTaskSubPO>()
                    .eq(OpenTaskSubPO::getId, row.getId())
                    .set(OpenTaskSubPO::getErrorMessage, null));
        }
    }
}
