package com.vtc.openapi.ui.admin;

import com.vtc.openapi.app.service.IPartnerAdminAppService;
import com.vtc.openapi.ui.dto.admin.CreateCredentialResponse;
import com.vtc.openapi.ui.dto.admin.CreatePartnerRequest;
import com.vtc.openapi.ui.dto.admin.PartnerDetailDto;
import com.vtc.openapi.ui.dto.admin.PartnerSummaryDto;
import com.vtc.openapi.ui.dto.admin.UpdatePartnerRequest;
import com.vtc.openapi.web.dto.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * Partner 内部管理 API（/internal/admin/partners · 运营后台）。
 * 鉴权：请求头 {@code X-Internal-Admin-Key}，见 {@code open-api.admin.api-key}。
 */
@RestController
@RequestMapping("/internal/admin/partners")
@Validated
@Api(tags = "Partner 内部管理")
public class PartnerAdminUI {

    private final IPartnerAdminAppService partnerAdminAppService;

    public PartnerAdminUI(IPartnerAdminAppService partnerAdminAppService) {
        this.partnerAdminAppService = partnerAdminAppService;
    }

    @ApiOperation("创建 Partner")
    @PostMapping
    public ApiResponse<PartnerDetailDto> createPartner(@Valid @RequestBody CreatePartnerRequest request) {
        return partnerAdminAppService.createPartner(request);
    }

    @ApiOperation("分页查询 Partner")
    @GetMapping
    public ApiResponse<List<PartnerSummaryDto>> listPartners(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return partnerAdminAppService.listPartners(page, size);
    }

    @ApiOperation("查询 Partner 详情")
    @GetMapping("/{partnerId}")
    public ApiResponse<PartnerDetailDto> getPartner(@PathVariable("partnerId") String partnerId) {
        return partnerAdminAppService.getPartner(partnerId);
    }

    @ApiOperation("更新 Partner（含 capabilities、callback、rateLimitQps、status）")
    @PutMapping("/{partnerId}")
    public ApiResponse<PartnerDetailDto> updatePartner(@PathVariable("partnerId") String partnerId,
                                                       @Valid @RequestBody UpdatePartnerRequest request) {
        return partnerAdminAppService.updatePartner(partnerId, request);
    }

    @ApiOperation(value = "创建 Partner 凭证", notes = "clientSecret 明文仅本次返回")
    @PostMapping("/{partnerId}/credentials")
    public ApiResponse<CreateCredentialResponse> createCredential(@PathVariable("partnerId") String partnerId) {
        return partnerAdminAppService.createCredential(partnerId);
    }
}
