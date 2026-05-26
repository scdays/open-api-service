package com.vtc.openapi.infra.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.infra.repository.DatabaseRepositoryImpl;
import com.botany.spore.ddd.infra.utils.convertor.ConvertHelper;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.entity.PartnerTaskMapDO;
import com.vtc.openapi.domain.task.model.query.OpenTaskListQuery;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.infra.dao.OpenTaskMapper;
import com.vtc.openapi.infra.dao.PartnerTaskMapMapper;
import com.vtc.openapi.infra.dao.po.OpenTaskPO;
import com.vtc.openapi.infra.dao.po.PartnerTaskMapPO;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class OpenTaskRepositoryImpl
        extends DatabaseRepositoryImpl<OpenTaskMapper, OpenTaskDO, OpenTaskPO>
        implements IOpenTaskRepository {

    private final PartnerTaskMapMapper partnerTaskMapMapper;

    public OpenTaskRepositoryImpl(PartnerTaskMapMapper partnerTaskMapMapper) {
        this.partnerTaskMapMapper = partnerTaskMapMapper;
    }

    @Override
    public OpenTaskDO findByTaskId(String taskId) {
        OpenTaskPO po = baseMapper.selectOne(new LambdaQueryWrapper<OpenTaskPO>()
                .eq(OpenTaskPO::getTaskId, taskId));
        return ConvertHelper.convert(po, OpenTaskDO.class);
    }

    @Override
    public PartnerTaskMapDO findTaskMap(String partnerId, String extTaskId) {
        PartnerTaskMapPO po = partnerTaskMapMapper.selectOne(new LambdaQueryWrapper<PartnerTaskMapPO>()
                .eq(PartnerTaskMapPO::getPartnerId, partnerId)
                .eq(PartnerTaskMapPO::getExtTaskId, extTaskId));
        return ConvertHelper.convert(po, PartnerTaskMapDO.class);
    }

    @Override
    public void saveTaskMap(PartnerTaskMapDO map) {
        partnerTaskMapMapper.insert(ConvertHelper.convert(map, PartnerTaskMapPO.class));
    }

    @Override
    public PageInfo<OpenTaskDO> pageByPartner(String partnerId, OpenTaskListQuery query) {
        LambdaQueryWrapper<OpenTaskPO> wrapper = new LambdaQueryWrapper<OpenTaskPO>()
                .eq(OpenTaskPO::getPartnerId, partnerId)
                .orderByDesc(OpenTaskPO::getCreatedAt);
        if (StringUtils.hasText(query.getExtTaskId())) {
            wrapper.eq(OpenTaskPO::getExtTaskId, query.getExtTaskId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(OpenTaskPO::getStatus, query.getStatus());
        }
        if (query.getCreatedFrom() != null) {
            wrapper.ge(OpenTaskPO::getCreatedAt, query.getCreatedFrom());
        }
        if (query.getCreatedTo() != null) {
            wrapper.le(OpenTaskPO::getCreatedAt, query.getCreatedTo());
        }
        Page<OpenTaskPO> pageResult = baseMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper);
        PageInfo<OpenTaskDO> pageInfo = new PageInfo<>();
        pageInfo.setCurrent(query.getPage());
        pageInfo.setSize(query.getSize());
        pageInfo.setTotal(pageResult.getTotal());
        pageInfo.setRecords(ConvertHelper.convertList(pageResult.getRecords(), OpenTaskDO.class));
        return pageInfo;
    }
}
