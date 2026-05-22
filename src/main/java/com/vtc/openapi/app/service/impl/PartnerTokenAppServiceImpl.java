package com.vtc.openapi.app.service.impl;

import com.alibaba.fastjson.JSON;
import com.botany.spore.core.utils.JWTUtils;
import com.vtc.openapi.app.service.IPartnerTokenAppService;
import com.vtc.openapi.common.OpenApiConstants;
import com.vtc.openapi.common.OpenApiException;
import com.vtc.openapi.domain.partner.model.entity.PartnerCredentialDO;
import com.vtc.openapi.domain.partner.model.entity.PartnerDO;
import com.vtc.openapi.infra.config.OpenApiProperties;
import com.vtc.openapi.infra.redis.PartnerTokenRedisStore;
import com.vtc.openapi.infra.repository.PartnerRepository;
import com.vtc.openapi.ui.dto.auth.PartnerTokenIntrospectRequest;
import com.vtc.openapi.ui.dto.auth.PartnerTokenIntrospectResponse;
import com.vtc.openapi.ui.dto.auth.PartnerTokenIssueRequest;
import com.vtc.openapi.ui.dto.auth.PartnerTokenIssueResponse;
import com.vtc.openapi.web.dto.ApiResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Partner Token 签发（对齐 clover {@code PartnerAppServiceImpl#issueToken}：JWT + Redis 上下文）。
 */
@Service
public class PartnerTokenAppServiceImpl implements IPartnerTokenAppService {

    private static final String GRANT_CLIENT_CREDENTIALS = "client_credentials";
    private static final String JWT_TYP_PARTNER = "partner";

    private final PartnerRepository partnerRepository;
    private final PartnerTokenRedisStore tokenRedisStore;
    private final OpenApiProperties properties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PartnerTokenAppServiceImpl(PartnerRepository partnerRepository,
                                      PartnerTokenRedisStore tokenRedisStore,
                                      OpenApiProperties properties) {
        this.partnerRepository = partnerRepository;
        this.tokenRedisStore = tokenRedisStore;
        this.properties = properties;
    }

    @Override
    public ApiResponse<PartnerTokenIssueResponse> issueToken(PartnerTokenIssueRequest request) {
        if (!GRANT_CLIENT_CREDENTIALS.equalsIgnoreCase(request.getGrantType())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "grantType 必须为 client_credentials");
        }
        PartnerCredentialDO credential = partnerRepository.findCredentialByClientId(request.getClientId());
        if (credential == null || !PartnerRepository.STATUS_ACTIVE.equals(credential.getStatus())) {
            return ApiResponse.of(OpenApiConstants.CODE_AUTH_FAILED, "Invalid client credentials", null);
        }
        if (!passwordEncoder.matches(request.getClientSecret(), credential.getClientSecretHash())) {
            return ApiResponse.of(OpenApiConstants.CODE_AUTH_FAILED, "Invalid client credentials", null);
        }

        PartnerDO partner = partnerRepository.findByPartnerId(credential.getPartnerId());
        if (partner == null || !PartnerRepository.STATUS_ACTIVE.equals(partner.getStatus())) {
            return ApiResponse.of(OpenApiConstants.CODE_AUTH_FAILED, "Partner 未激活或不存在", null);
        }

        List<String> capabilities = partnerRepository.listCapabilities(partner.getPartnerId());
        long expiresIn = properties.getToken().getExpiresInSeconds();
        long nowEpoch = System.currentTimeMillis() / 1000;
        long expiresAt = nowEpoch + expiresIn;

        PartnerTokenIntrospectResponse cache = new PartnerTokenIntrospectResponse();
        cache.setSubjectType(OpenApiConstants.SUBJECT_TYPE_PARTNER);
        cache.setPartnerId(partner.getPartnerId());
        cache.setCapabilities(capabilities);
        cache.setClientId(credential.getClientId());
        cache.setIssuedAt(nowEpoch);
        cache.setExpiresAt(expiresAt);

        String accessToken = createAccessToken(partner.getPartnerId(), credential.getClientId(), capabilities, expiresIn);
        tokenRedisStore.saveToken(accessToken, cache, expiresIn);

        PartnerTokenIssueResponse data = new PartnerTokenIssueResponse();
        data.setAccessToken(accessToken);
        data.setTokenType("Bearer");
        data.setExpiresIn((int) expiresIn);
        data.setPartnerId(partner.getPartnerId());
        return ApiResponse.ok(data);
    }

    @Override
    public ApiResponse<PartnerTokenIntrospectResponse> introspect(PartnerTokenIntrospectRequest request) {
        if (request == null || !StringUtils.hasText(request.getToken())) {
            return ApiResponse.of(OpenApiConstants.CODE_AUTH_FAILED, "token 无效", null);
        }
        String bearer = stripBearerPrefix(request.getToken());
        PartnerTokenIntrospectResponse cached = tokenRedisStore.getByToken(bearer);
        if (cached == null) {
            return ApiResponse.of(OpenApiConstants.CODE_AUTH_FAILED, "token 无效或已过期", null);
        }
        Long expiresAt = cached.getExpiresAt();
        if (expiresAt != null && expiresAt > 0 && expiresAt < System.currentTimeMillis() / 1000) {
            return ApiResponse.of(OpenApiConstants.CODE_AUTH_FAILED, "token 已过期", null);
        }
        if (!OpenApiConstants.SUBJECT_TYPE_PARTNER.equalsIgnoreCase(cached.getSubjectType())) {
            return ApiResponse.of(OpenApiConstants.CODE_AUTH_FAILED, "subjectType 无效", null);
        }
        return ApiResponse.ok(cached);
    }

    /**
     * 对齐 clover {@code PartnerAppServiceImpl#createAccessToken}：JWT subject 为 claims JSON。
     */
    private String createAccessToken(String partnerId, String clientId, List<String> capabilities, long expiresInSeconds) {
        Map<String, Object> claims = new HashMap<>(4);
        claims.put("sub", partnerId);
        claims.put("typ", JWT_TYP_PARTNER);
        claims.put("capabilities", capabilities);
        claims.put("clientId", clientId);
        return JWTUtils.create(JSON.toJSONString(claims), expiresInSeconds * 1000L);
    }

    private String stripBearerPrefix(String token) {
        String trimmed = token.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return trimmed.substring(7).trim();
        }
        return trimmed;
    }
}
