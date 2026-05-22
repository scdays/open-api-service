package com.vtc.openapi.app.service.impl;

import com.vtc.openapi.app.service.IPartnerAdminAppService;
import com.vtc.openapi.common.OpenApiConstants;
import com.vtc.openapi.common.OpenApiException;
import com.vtc.openapi.domain.partner.model.entity.PartnerCredentialDO;
import com.vtc.openapi.domain.partner.model.entity.PartnerDO;
import com.vtc.openapi.infra.redis.PartnerTokenRedisStore;
import com.vtc.openapi.infra.repository.PartnerRepository;
import com.vtc.openapi.ui.dto.admin.CreateCredentialResponse;
import com.vtc.openapi.ui.dto.admin.CreatePartnerRequest;
import com.vtc.openapi.ui.dto.admin.PartnerDetailDto;
import com.vtc.openapi.ui.dto.admin.PartnerSummaryDto;
import com.vtc.openapi.ui.dto.admin.UpdatePartnerRequest;
import com.vtc.openapi.web.dto.ApiResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PartnerAdminAppServiceImpl implements IPartnerAdminAppService {

    private final PartnerRepository partnerRepository;
    private final PartnerTokenRedisStore tokenRedisStore;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public PartnerAdminAppServiceImpl(PartnerRepository partnerRepository,
                                      PartnerTokenRedisStore tokenRedisStore) {
        this.partnerRepository = partnerRepository;
        this.tokenRedisStore = tokenRedisStore;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<PartnerDetailDto> createPartner(CreatePartnerRequest request) {
        if (partnerRepository.findByPartnerId(request.getPartnerId()) != null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "partnerId 已存在");
        }
        Date now = new Date();
        PartnerDO partner = new PartnerDO();
        partner.setPartnerId(request.getPartnerId());
        partner.setPartnerName(request.getPartnerName());
        partner.setPartnerType(request.getPartnerType());
        partner.setStatus(PartnerRepository.STATUS_ACTIVE);
        partner.setRateLimitQps(request.getRateLimitQps() != null ? request.getRateLimitQps() : 100);
        partner.setCreatedAt(now);
        partner.setUpdatedAt(now);
        try {
            partnerRepository.insertPartner(partner);
        } catch (DuplicateKeyException ex) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "partnerId 已存在");
        }
        if (!CollectionUtils.isEmpty(request.getCapabilities())) {
            partnerRepository.replaceCapabilities(request.getPartnerId(), request.getCapabilities());
        }
        partnerRepository.upsertCallbackUrl(request.getPartnerId(), request.getDefaultCallbackUrl());
        return ApiResponse.ok(toDetailDto(partner));
    }

    @Override
    public ApiResponse<List<PartnerSummaryDto>> listPartners(int page, int size) {
        if (page < 1 || size < 1) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "page/size 无效");
        }
        int offset = (page - 1) * size;
        List<PartnerDO> partners = partnerRepository.listPartners(offset, size);
        List<PartnerSummaryDto> items = partners.stream().map(this::toSummaryDto).collect(Collectors.toList());
        return ApiResponse.ok(items);
    }

    @Override
    public ApiResponse<PartnerDetailDto> getPartner(String partnerId) {
        PartnerDO partner = requirePartner(partnerId);
        return ApiResponse.ok(toDetailDto(partner));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<PartnerDetailDto> updatePartner(String partnerId, UpdatePartnerRequest request) {
        PartnerDO partner = requirePartner(partnerId);
        if (StringUtils.hasText(request.getPartnerName())) {
            partner.setPartnerName(request.getPartnerName());
        }
        if (StringUtils.hasText(request.getStatus())) {
            partner.setStatus(request.getStatus());
        }
        if (request.getRateLimitQps() != null) {
            partner.setRateLimitQps(request.getRateLimitQps());
        }
        partnerRepository.updatePartner(partner);
        if (request.getCapabilities() != null) {
            partnerRepository.replaceCapabilities(partnerId, request.getCapabilities());
        }
        if (request.getDefaultCallbackUrl() != null) {
            partnerRepository.upsertCallbackUrl(partnerId, request.getDefaultCallbackUrl());
        }
        return ApiResponse.ok(toDetailDto(partner));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<CreateCredentialResponse> createCredential(String partnerId) {
        PartnerDO partner = requirePartner(partnerId);
        if (!PartnerRepository.STATUS_ACTIVE.equals(partner.getStatus())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "Partner 非 ACTIVE，禁止签发凭证");
        }
        String clientId = "cli_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String clientSecret = generateClientSecret();
        Date now = new Date();

        PartnerCredentialDO credential = new PartnerCredentialDO();
        credential.setPartnerId(partnerId);
        credential.setClientId(clientId);
        credential.setClientSecretHash(passwordEncoder.encode(clientSecret));
        credential.setStatus(PartnerRepository.STATUS_ACTIVE);
        credential.setCreatedAt(now);
        credential.setUpdatedAt(now);
        partnerRepository.insertCredential(credential);
        tokenRedisStore.saveCredentialMeta(clientId, partnerId, PartnerRepository.STATUS_ACTIVE);

        CreateCredentialResponse data = new CreateCredentialResponse();
        data.setPartnerId(partnerId);
        data.setClientId(clientId);
        data.setClientSecret(clientSecret);
        data.setStatus(PartnerRepository.STATUS_ACTIVE);
        return ApiResponse.ok(data);
    }

    private PartnerDO requirePartner(String partnerId) {
        PartnerDO partner = partnerRepository.findByPartnerId(partnerId);
        if (partner == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "Partner 不存在");
        }
        return partner;
    }

    private PartnerDetailDto toDetailDto(PartnerDO partner) {
        PartnerDetailDto dto = new PartnerDetailDto();
        dto.setPartnerId(partner.getPartnerId());
        dto.setPartnerName(partner.getPartnerName());
        dto.setPartnerType(partner.getPartnerType());
        dto.setStatus(partner.getStatus());
        dto.setRateLimitQps(partner.getRateLimitQps());
        dto.setCapabilities(new ArrayList<>(partnerRepository.listCapabilities(partner.getPartnerId())));
        dto.setDefaultCallbackUrl(partnerRepository.findCallbackUrl(partner.getPartnerId()));
        return dto;
    }

    private PartnerSummaryDto toSummaryDto(PartnerDO partner) {
        PartnerSummaryDto dto = new PartnerSummaryDto();
        dto.setPartnerId(partner.getPartnerId());
        dto.setPartnerName(partner.getPartnerName());
        dto.setStatus(partner.getStatus());
        return dto;
    }

    private String generateClientSecret() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
