package com.vtc.openapi.infra.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.infra.utils.convertor.ConvertHelper;
import com.vtc.openapi.domain.operationcase.model.entity.OpenOperationCaseDO;
import com.vtc.openapi.domain.operationcase.model.entity.OpenOperationCaseEventDO;
import com.vtc.openapi.domain.operationcase.model.entity.OpenOperationCaseTargetDO;
import com.vtc.openapi.domain.operationcase.model.query.OperationCaseAdminQuery;
import com.vtc.openapi.domain.operationcase.repository.IOpenOperationCaseRepository;
import com.vtc.openapi.infra.dao.OpenOperationCaseEventMapper;
import com.vtc.openapi.infra.dao.OpenOperationCaseMapper;
import com.vtc.openapi.infra.dao.OpenOperationCaseTargetMapper;
import com.vtc.openapi.infra.dao.po.OpenOperationCaseEventPO;
import com.vtc.openapi.infra.dao.po.OpenOperationCasePO;
import com.vtc.openapi.infra.dao.po.OpenOperationCaseTargetPO;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class OpenOperationCaseRepositoryImpl implements IOpenOperationCaseRepository {

    private final OpenOperationCaseMapper caseMapper;
    private final OpenOperationCaseEventMapper eventMapper;
    private final OpenOperationCaseTargetMapper targetMapper;

    public OpenOperationCaseRepositoryImpl(OpenOperationCaseMapper caseMapper,
                                           OpenOperationCaseEventMapper eventMapper,
                                           OpenOperationCaseTargetMapper targetMapper) {
        this.caseMapper = caseMapper;
        this.eventMapper = eventMapper;
        this.targetMapper = targetMapper;
    }

    @Override
    public void insert(OpenOperationCaseDO row) {
        caseMapper.insert(ConvertHelper.convert(row, OpenOperationCasePO.class));
    }

    @Override
    public void updateOnFinish(OpenOperationCaseDO patch) {
        if (!StringUtils.hasText(patch.getCaseId())) {
            return;
        }
        OpenOperationCasePO row = new OpenOperationCasePO();
        row.setCaseId(patch.getCaseId());
        row.setStatus(patch.getStatus());
        row.setPrimaryResourceType(patch.getPrimaryResourceType());
        row.setPrimaryResourceId(patch.getPrimaryResourceId());
        row.setBatchId(patch.getBatchId());
        row.setResultSummaryJson(patch.getResultSummaryJson());
        row.setErrorMessage(patch.getErrorMessage());
        row.setFinishedAt(patch.getFinishedAt());
        row.setUpdatedAt(patch.getUpdatedAt());
        caseMapper.updateById(row);
    }

    @Override
    public void updateProgress(OpenOperationCaseDO patch) {
        if (!StringUtils.hasText(patch.getCaseId())) {
            return;
        }
        OpenOperationCasePO row = new OpenOperationCasePO();
        row.setCaseId(patch.getCaseId());
        row.setStatus(patch.getStatus());
        row.setPrimaryResourceType(patch.getPrimaryResourceType());
        row.setPrimaryResourceId(patch.getPrimaryResourceId());
        row.setErrorMessage(patch.getErrorMessage());
        row.setFinishedAt(patch.getFinishedAt());
        row.setUpdatedAt(patch.getUpdatedAt());
        caseMapper.updateById(row);
    }

    @Override
    public OpenOperationCaseDO findByCaseId(String caseId) {
        if (!StringUtils.hasText(caseId)) {
            return null;
        }
        return ConvertHelper.convert(caseMapper.selectById(caseId), OpenOperationCaseDO.class);
    }

    @Override
    public OpenOperationCaseDO findByInvocationId(String invocationId) {
        if (!StringUtils.hasText(invocationId)) {
            return null;
        }
        LambdaQueryWrapper<OpenOperationCasePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OpenOperationCasePO::getInvocationId, invocationId.trim());
        wrapper.last("LIMIT 1");
        return ConvertHelper.convert(caseMapper.selectOne(wrapper), OpenOperationCaseDO.class);
    }

    @Override
    public OpenOperationCaseDO findByPrimaryResource(String partnerId, String primaryResourceType,
                                                      String primaryResourceId) {
        if (!StringUtils.hasText(primaryResourceType) || !StringUtils.hasText(primaryResourceId)) {
            return null;
        }
        LambdaQueryWrapper<OpenOperationCasePO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(partnerId)) {
            wrapper.eq(OpenOperationCasePO::getPartnerId, partnerId.trim());
        }
        wrapper.eq(OpenOperationCasePO::getPrimaryResourceType, primaryResourceType.trim());
        wrapper.eq(OpenOperationCasePO::getPrimaryResourceId, primaryResourceId.trim());
        wrapper.orderByDesc(OpenOperationCasePO::getStartedAt);
        wrapper.last("LIMIT 1");
        return ConvertHelper.convert(caseMapper.selectOne(wrapper), OpenOperationCaseDO.class);
    }

    @Override
    public List<OpenOperationCaseDO> listRecent(String partnerId, int limit) {
        int capped = limit > 0 ? Math.min(limit, 500) : 200;
        LambdaQueryWrapper<OpenOperationCasePO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(partnerId)) {
            wrapper.eq(OpenOperationCasePO::getPartnerId, partnerId.trim());
        }
        wrapper.orderByDesc(OpenOperationCasePO::getStartedAt);
        wrapper.last("LIMIT " + capped);
        List<OpenOperationCasePO> rows = caseMapper.selectList(wrapper);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream()
                .map(po -> ConvertHelper.convert(po, OpenOperationCaseDO.class))
                .collect(Collectors.toList());
    }

    @Override
    public PageInfo<OpenOperationCaseDO> pageCases(OperationCaseAdminQuery query) {
        LambdaQueryWrapper<OpenOperationCasePO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getPartnerId())) {
            wrapper.eq(OpenOperationCasePO::getPartnerId, query.getPartnerId());
        }
        if (StringUtils.hasText(query.getCaseType())) {
            wrapper.eq(OpenOperationCasePO::getCaseType, query.getCaseType());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(OpenOperationCasePO::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getPrimaryResourceId())) {
            wrapper.eq(OpenOperationCasePO::getPrimaryResourceId, query.getPrimaryResourceId());
        }
        if (StringUtils.hasText(query.getCaseId())) {
            wrapper.eq(OpenOperationCasePO::getCaseId, query.getCaseId());
        }
        if (query.getStartedFrom() != null) {
            wrapper.ge(OpenOperationCasePO::getStartedAt, query.getStartedFrom());
        }
        if (query.getStartedTo() != null) {
            wrapper.le(OpenOperationCasePO::getStartedAt, query.getStartedTo());
        }
        wrapper.orderByDesc(OpenOperationCasePO::getStartedAt);

        int page = Math.max(query.getPage(), 1);
        int size = query.getSize() > 0 ? query.getSize() : 20;
        Page<OpenOperationCasePO> mpPage = caseMapper.selectPage(new Page<>(page, size), wrapper);

        PageInfo<OpenOperationCaseDO> result = new PageInfo<>();
        result.setCurrent(page);
        result.setSize(size);
        result.setTotal(mpPage.getTotal());
        if (mpPage.getRecords() == null || mpPage.getRecords().isEmpty()) {
            result.setRecords(Collections.emptyList());
        } else {
            result.setRecords(mpPage.getRecords().stream()
                    .map(po -> ConvertHelper.convert(po, OpenOperationCaseDO.class))
                    .collect(Collectors.toList()));
        }
        return result;
    }

    @Override
    public void insertEvent(OpenOperationCaseEventDO event) {
        eventMapper.insert(ConvertHelper.convert(event, OpenOperationCaseEventPO.class));
    }

    @Override
    public List<OpenOperationCaseEventDO> listEventsByCaseId(String caseId) {
        if (!StringUtils.hasText(caseId)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<OpenOperationCaseEventPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OpenOperationCaseEventPO::getCaseId, caseId);
        wrapper.orderByAsc(OpenOperationCaseEventPO::getCreatedAt);
        List<OpenOperationCaseEventPO> rows = eventMapper.selectList(wrapper);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream()
                .map(po -> ConvertHelper.convert(po, OpenOperationCaseEventDO.class))
                .collect(Collectors.toList());
    }

    @Override
    public void insertTargets(List<OpenOperationCaseTargetDO> targets) {
        if (CollectionUtils.isEmpty(targets)) {
            return;
        }
        for (OpenOperationCaseTargetDO target : targets) {
            targetMapper.insert(ConvertHelper.convert(target, OpenOperationCaseTargetPO.class));
        }
    }

    @Override
    public List<OpenOperationCaseTargetDO> listTargetsByCaseId(String caseId) {
        if (!StringUtils.hasText(caseId)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<OpenOperationCaseTargetPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OpenOperationCaseTargetPO::getCaseId, caseId.trim());
        wrapper.orderByAsc(OpenOperationCaseTargetPO::getId);
        List<OpenOperationCaseTargetPO> rows = targetMapper.selectList(wrapper);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream()
                .map(po -> ConvertHelper.convert(po, OpenOperationCaseTargetDO.class))
                .collect(Collectors.toList());
    }
}
