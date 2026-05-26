package com.vtc.openapi.app.service.impl;

import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.infra.utils.convertor.ConvertHelper;
import com.vtc.openapi.app.service.IApiCatalogAdminAppService;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.open.model.entity.ApiOperationDO;
import com.vtc.openapi.domain.open.model.query.ApiOperationAdminQuery;
import com.vtc.openapi.domain.open.service.business.IApiCatalogDomainService;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.ApiOperationDTO;
import com.vtc.openapi.ui.dto.admin.ApiOperationPageDto;
import org.springframework.stereotype.Service;

@Service
public class ApiCatalogAdminAppServiceImpl implements IApiCatalogAdminAppService {

    private final IApiCatalogDomainService apiCatalogDomainService;

    public ApiCatalogAdminAppServiceImpl(IApiCatalogDomainService apiCatalogDomainService) {
        this.apiCatalogDomainService = apiCatalogDomainService;
    }

    @Override
    public ApiResponse<ApiOperationPageDto> listApiOperations(PageInfo<ApiOperationDTO> pageInfo,
                                                              String requiredCapability,
                                                              String status,
                                                              String openapiTag,
                                                              String domain,
                                                              String operationId) {
        if (pageInfo.getCurrent() < 1 || pageInfo.getSize() < 1) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "page/size 无效");
        }
        ApiOperationAdminQuery query = new ApiOperationAdminQuery();
        query.setRequiredCapability(requiredCapability);
        query.setStatus(status);
        query.setOpenapiTag(openapiTag);
        query.setDomain(domain);
        query.setOperationId(operationId);
        query.setPage((int) pageInfo.getCurrent());
        query.setSize((int) pageInfo.getSize());

        PageInfo<ApiOperationDO> resultPage = apiCatalogDomainService.pageApiOperations(query);
        ApiOperationPageDto dto = new ApiOperationPageDto();
        dto.setItems(ConvertHelper.convertList(resultPage.getRecords(), ApiOperationDTO.class));
        dto.setPage((int) resultPage.getCurrent());
        dto.setSize((int) resultPage.getSize());
        dto.setTotal(resultPage.getTotal());
        return ApiResponse.ok(dto);
    }
}
