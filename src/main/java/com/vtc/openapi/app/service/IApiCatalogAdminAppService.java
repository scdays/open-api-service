package com.vtc.openapi.app.service;

import com.botany.spore.core.page.PageInfo;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.ApiOperationDTO;
import com.vtc.openapi.ui.dto.admin.ApiOperationPageDto;

public interface IApiCatalogAdminAppService {

    ApiResponse<ApiOperationPageDto> listApiOperations(PageInfo<ApiOperationDTO> pageInfo,
                                                       String requiredCapability,
                                                       String status,
                                                       String openapiTag,
                                                       String domain,
                                                       String operationId,
                                                       String keyword);
}
