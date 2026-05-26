package com.vtc.openapi.ui.admin;

import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.ui.BaseUI;
import com.vtc.openapi.app.service.IInvocationAdminAppService;
import com.vtc.openapi.app.service.IPartnerAdminAppService;
import com.vtc.openapi.ui.dto.admin.PartnerCredentialDTO;
import com.vtc.openapi.ui.dto.admin.PartnerDTO;
import com.vtc.openapi.ui.dto.admin.PartnerInvocationStatsDto;
import com.vtc.openapi.ui.dto.admin.PartnerPageDto;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.params.admin.CreatePartnerParams;
import com.vtc.openapi.ui.params.admin.UpdatePartnerParams;
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
 */
@RestController
@RequestMapping("/internal/admin/partners")
@Validated
@Api(tags = "Partner 内部管理")
public class PartnerAdminUI extends BaseUI {

    private final IPartnerAdminAppService partnerAdminAppService;
    private final IInvocationAdminAppService invocationAdminAppService;

    public PartnerAdminUI(IPartnerAdminAppService partnerAdminAppService,
                          IInvocationAdminAppService invocationAdminAppService) {
        this.partnerAdminAppService = partnerAdminAppService;
        this.invocationAdminAppService = invocationAdminAppService;
    }

    @ApiOperation("创建 Partner")
    @PostMapping
    public ApiResponse<PartnerDTO> createPartner(@Valid @RequestBody CreatePartnerParams params) {
        return partnerAdminAppService.createPartner(params);
    }

    @ApiOperation("分页查询 Partner")
    @GetMapping
    public ApiResponse<PartnerPageDto> listPartners(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        PageInfo<PartnerDTO> pageInfo = getPageInfo(page, size);
        return partnerAdminAppService.listPartners(pageInfo);
    }

    @ApiOperation("查询 Partner 详情")
    @GetMapping("/{partnerId}")
    public ApiResponse<PartnerDTO> getPartner(@PathVariable("partnerId") String partnerId) {
        return partnerAdminAppService.getPartner(partnerId);
    }

    @ApiOperation("更新 Partner（含 capabilities、callback、rateLimitQps、status）")
    @PutMapping("/{partnerId}")
    public ApiResponse<PartnerDTO> updatePartner(@PathVariable("partnerId") String partnerId,
                                                 @Valid @RequestBody UpdatePartnerParams params) {
        return partnerAdminAppService.updatePartner(partnerId, params);
    }

    @ApiOperation(value = "创建 Partner 凭证", notes = "clientSecret 明文仅本次返回")
    @PostMapping("/{partnerId}/credentials")
    public ApiResponse<PartnerCredentialDTO> createCredential(@PathVariable("partnerId") String partnerId) {
        return partnerAdminAppService.createCredential(partnerId);
    }

    @ApiOperation(value = "查询 Partner 凭证列表", notes = "不含 clientSecret")
    @GetMapping("/{partnerId}/credentials")
    public ApiResponse<List<PartnerCredentialDTO>> listCredentials(@PathVariable("partnerId") String partnerId) {
        return partnerAdminAppService.listCredentials(partnerId);
    }

    @ApiOperation("查询 Partner 调用统计")
    @GetMapping("/{partnerId}/stats")
    public ApiResponse<PartnerInvocationStatsDto> getPartnerStats(@PathVariable("partnerId") String partnerId) {
        return invocationAdminAppService.getPartnerStats(partnerId);
    }
}
