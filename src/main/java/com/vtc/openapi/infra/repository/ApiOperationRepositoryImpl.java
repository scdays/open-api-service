package com.vtc.openapi.infra.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.infra.utils.convertor.ConvertHelper;
import com.vtc.openapi.domain.open.model.entity.ApiOperationDO;
import com.vtc.openapi.domain.open.model.query.ApiOperationAdminQuery;
import com.vtc.openapi.domain.open.repository.IApiOperationRepository;
import com.vtc.openapi.infra.dao.ApiOperationMapper;
import com.vtc.openapi.infra.dao.po.ApiOperationPO;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class ApiOperationRepositoryImpl implements IApiOperationRepository {

    private final ApiOperationMapper apiOperationMapper;

    public ApiOperationRepositoryImpl(ApiOperationMapper apiOperationMapper) {
        this.apiOperationMapper = apiOperationMapper;
    }

    @Override
    public ApiOperationDO findByOperationId(String operationId) {
        ApiOperationPO po = apiOperationMapper.selectOne(new LambdaQueryWrapper<ApiOperationPO>()
                .eq(ApiOperationPO::getOperationId, operationId));
        return ConvertHelper.convert(po, ApiOperationDO.class);
    }

    @Override
    public PageInfo<ApiOperationDO> pageApiOperations(ApiOperationAdminQuery query) {
        LambdaQueryWrapper<ApiOperationPO> wrapper = new LambdaQueryWrapper<ApiOperationPO>()
                .orderByDesc(ApiOperationPO::getPublishedAt)
                .orderByAsc(ApiOperationPO::getOperationId);
        if (StringUtils.hasText(query.getRequiredCapability())) {
            wrapper.eq(ApiOperationPO::getRequiredCapability, query.getRequiredCapability());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(ApiOperationPO::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getOpenapiTag())) {
            wrapper.eq(ApiOperationPO::getOpenapiTag, query.getOpenapiTag());
        }
        if (StringUtils.hasText(query.getDomain())) {
            wrapper.eq(ApiOperationPO::getDomain, query.getDomain());
        }
        if (StringUtils.hasText(query.getOperationId())) {
            wrapper.eq(ApiOperationPO::getOperationId, query.getOperationId());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(ApiOperationPO::getOperationId, keyword)
                    .or().like(ApiOperationPO::getPathPattern, keyword)
                    .or().like(ApiOperationPO::getSummary, keyword));
        }

        Page<ApiOperationPO> pageResult = apiOperationMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper);
        PageInfo<ApiOperationDO> pageInfo = new PageInfo<>();
        pageInfo.setCurrent(query.getPage());
        pageInfo.setSize(query.getSize());
        pageInfo.setTotal(pageResult.getTotal());
        pageInfo.setRecords(ConvertHelper.convertList(pageResult.getRecords(), ApiOperationDO.class));
        return pageInfo;
    }
}
