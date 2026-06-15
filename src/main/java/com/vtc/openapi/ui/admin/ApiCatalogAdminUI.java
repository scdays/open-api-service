package com.vtc.openapi.ui.admin;

import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.ui.BaseUI;
import com.vtc.openapi.app.service.IApiCatalogAdminAppService;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.ApiOperationDTO;
import com.vtc.openapi.ui.dto.admin.ApiOperationPageDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/admin")
@Validated
@Api(tags = "API 目录内部管理")
public class ApiCatalogAdminUI extends BaseUI {

    private final IApiCatalogAdminAppService apiCatalogAdminAppService;

    public ApiCatalogAdminUI(IApiCatalogAdminAppService apiCatalogAdminAppService) {
        this.apiCatalogAdminAppService = apiCatalogAdminAppService;
    }

    @ApiOperation("分页查询 API 目录")
    @GetMapping("/api-operations")
    public ApiResponse<ApiOperationPageDto> listApiOperations(
            @RequestParam(value = "requiredCapability", required = false) String requiredCapability,
            @RequestParam(value = "capabilityCode", required = false) String capabilityCode,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "openapiTag", required = false) String openapiTag,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "domain", required = false) String domain,
            @RequestParam(value = "operationId", required = false) String operationId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        PageInfo<ApiOperationDTO> pageInfo = getPageInfo(page, size);
        String resolvedCapability = org.springframework.util.StringUtils.hasText(requiredCapability)
                ? requiredCapability : capabilityCode;
        String resolvedTag = org.springframework.util.StringUtils.hasText(openapiTag) ? openapiTag : tag;
        return apiCatalogAdminAppService.listApiOperations(
                pageInfo, resolvedCapability, status, resolvedTag, domain, operationId, keyword);
    }
}
