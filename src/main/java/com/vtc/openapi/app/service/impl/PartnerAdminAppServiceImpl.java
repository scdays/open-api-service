package com.vtc.openapi.app.service.impl;

import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.app.service.AppServiceImpl;
import com.botany.spore.ddd.infra.utils.convertor.ConvertHelper;
import com.vtc.openapi.app.service.IPartnerAdminAppService;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.partner.model.PartnerConstants;
import com.vtc.openapi.domain.partner.model.entity.PartnerCredentialDO;
import com.vtc.openapi.domain.partner.model.entity.PartnerDO;
import com.vtc.openapi.domain.partner.service.business.IPartnerDomainService;
import com.vtc.openapi.infra.redis.PartnerTokenRedisStore;
import com.vtc.openapi.infra.webhook.WebhookCallbackUrlResolver;
import com.vtc.openapi.ui.dto.admin.PartnerCredentialDTO;
import com.vtc.openapi.ui.dto.admin.PartnerDTO;
import com.vtc.openapi.ui.dto.admin.PartnerPageDto;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.params.admin.CreatePartnerParams;
import com.vtc.openapi.ui.params.admin.UpdatePartnerParams;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Partner 内部管理应用服务（继承 AppServiceImpl · ConvertHelper 转换）。
 */
@Service
public class PartnerAdminAppServiceImpl
        extends AppServiceImpl<IPartnerDomainService, PartnerDO, PartnerDTO>
        implements IPartnerAdminAppService {

    private final PartnerTokenRedisStore tokenRedisStore;
    private final WebhookCallbackUrlResolver callbackUrlResolver;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public PartnerAdminAppServiceImpl(IPartnerDomainService partnerDomainService,
                                      PartnerTokenRedisStore tokenRedisStore,
                                      WebhookCallbackUrlResolver callbackUrlResolver) {
        this.domainService = partnerDomainService;
        this.tokenRedisStore = tokenRedisStore;
        this.callbackUrlResolver = callbackUrlResolver;
    }

    @Override
    public ApiResponse<PartnerDTO> createPartner(CreatePartnerParams params) {
        PartnerDO toCreate = ConvertHelper.convert(fromCreateParams(params), PartnerDO.class);
        PartnerDO saved = domainService.createPartner(
                toCreate, params.getCapabilities(), callbackUrlResolver.resolveForCreate(params.getDefaultCallbackUrl()));
        return ApiResponse.ok(enrichDetail(saved));
    }

    @Override
    public ApiResponse<PartnerPageDto> listPartners(PageInfo<PartnerDTO> pageInfo) {
        if (pageInfo.getCurrent() < 1 || pageInfo.getSize() < 1) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "page/size 无效");
        }
        PageInfo<PartnerDO> doPage = ConvertHelper.convertPageInfo(pageInfo, PartnerDO.class);
        PageInfo<PartnerDO> page = domainService.pagePartners(doPage);
        List<PartnerDTO> items = page.getRecords().stream()
                .map(this::enrichSummary)
                .collect(Collectors.toList());
        PartnerPageDto pageDto = new PartnerPageDto();
        pageDto.setItems(items);
        pageDto.setTotal(page.getTotal());
        pageDto.setPage((int) page.getCurrent());
        pageDto.setSize((int) page.getSize());
        return ApiResponse.ok(pageDto);
    }

    @Override
    public ApiResponse<PartnerDTO> getPartner(String partnerId) {
        return ApiResponse.ok(enrichDetail(domainService.requireByPartnerId(partnerId)));
    }

    @Override
    public ApiResponse<PartnerDTO> updatePartner(String partnerId, UpdatePartnerParams params) {
        PartnerDO patch = ConvertHelper.convert(fromUpdateParams(params), PartnerDO.class);
        PartnerDO updated = domainService.updatePartner(
                partnerId, patch, params.getCapabilities(), params.getDefaultCallbackUrl());
        return ApiResponse.ok(enrichDetail(updated));
    }

    @Override
    public ApiResponse<PartnerCredentialDTO> createCredential(String partnerId) {
        String clientId = "cli_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String clientSecret = generateClientSecret();
        PartnerCredentialDO saved = domainService.createCredential(
                partnerId, clientId, passwordEncoder.encode(clientSecret));
        tokenRedisStore.saveCredentialMeta(clientId, partnerId, PartnerConstants.STATUS_ACTIVE);

        PartnerCredentialDTO data = ConvertHelper.convert(saved, PartnerCredentialDTO.class);
        data.setPartnerId(partnerId);
        data.setClientSecret(clientSecret);
        return ApiResponse.ok(data);
    }

    @Override
    public ApiResponse<List<PartnerCredentialDTO>> listCredentials(String partnerId) {
        List<PartnerCredentialDTO> items = ConvertHelper.convertList(
                domainService.listCredentials(partnerId), PartnerCredentialDTO.class);
        return ApiResponse.ok(items);
    }

    private PartnerDTO fromCreateParams(CreatePartnerParams params) {
        PartnerDTO dto = new PartnerDTO();
        dto.setPartnerId(params.getPartnerId());
        dto.setPartnerName(params.getPartnerName());
        dto.setPartnerType(params.getPartnerType());
        dto.setRateLimitQps(params.getRateLimitQps());
        return dto;
    }

    private PartnerDTO fromUpdateParams(UpdatePartnerParams params) {
        PartnerDTO dto = new PartnerDTO();
        dto.setPartnerName(params.getPartnerName());
        dto.setStatus(params.getStatus());
        dto.setRateLimitQps(params.getRateLimitQps());
        return dto;
    }

    private PartnerDTO enrichDetail(PartnerDO partner) {
        PartnerDTO dto = ConvertHelper.convert(partner, PartnerDTO.class);
        dto.setCapabilities(new ArrayList<>(domainService.listCapabilities(partner.getPartnerId())));
        dto.setDefaultCallbackUrl(domainService.findCallbackUrl(partner.getPartnerId()));
        return dto;
    }

    private PartnerDTO enrichSummary(PartnerDO partner) {
        PartnerDTO dto = ConvertHelper.convert(partner, PartnerDTO.class);
        dto.setCapabilities(new ArrayList<>(domainService.listCapabilities(partner.getPartnerId())));
        return dto;
    }

    private String generateClientSecret() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
