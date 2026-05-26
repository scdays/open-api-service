package com.vtc.openapi.domain.open.repository;

import com.botany.spore.core.page.PageInfo;
import com.vtc.openapi.domain.open.model.entity.ApiOperationDO;
import com.vtc.openapi.domain.open.model.query.ApiOperationAdminQuery;

public interface IApiOperationRepository {

    ApiOperationDO findByOperationId(String operationId);

    PageInfo<ApiOperationDO> pageApiOperations(ApiOperationAdminQuery query);
}
