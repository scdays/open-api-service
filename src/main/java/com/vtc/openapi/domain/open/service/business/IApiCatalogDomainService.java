package com.vtc.openapi.domain.open.service.business;

import com.botany.spore.core.page.PageInfo;
import com.vtc.openapi.domain.open.model.entity.ApiOperationDO;
import com.vtc.openapi.domain.open.model.query.ApiOperationAdminQuery;

/**
 * API 目录（api_operation）领域服务。
 */
public interface IApiCatalogDomainService {

    void requirePublished(String operationId);

    PageInfo<ApiOperationDO> pageApiOperations(ApiOperationAdminQuery query);
}
