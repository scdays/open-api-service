package com.vtc.openapi.app.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.botany.spore.core.utils.JWTUtils;
import com.vtc.openapi.app.service.IPartnerTokenAppService;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.open.OpenApiOperations;
import com.vtc.openapi.domain.open.model.InvocationContext;
import com.vtc.openapi.domain.open.service.business.IInvocationDomainService;
import com.vtc.openapi.domain.partner.context.PartnerContext;
import com.vtc.openapi.domain.partner.model.PartnerConstants;
import com.vtc.openapi.domain.partner.model.entity.PartnerCredentialDO;
import com.vtc.openapi.domain.partner.model.entity.PartnerDO;
import com.vtc.openapi.domain.partner.service.business.IPartnerDomainService;
import com.vtc.openapi.infra.config.OpenApiProperties;
import com.vtc.openapi.infra.redis.PartnerTokenRedisStore;
import com.vtc.openapi.ui.dto.auth.PartnerTokenIntrospectRequest;
import com.vtc.openapi.ui.dto.auth.PartnerTokenIntrospectResponse;
import com.vtc.openapi.ui.dto.auth.PartnerTokenIssueRequest;
import com.vtc.openapi.ui.dto.auth.PartnerTokenIssueResponse;
import com.vtc.openapi.ui.dto.ApiResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Partner Token 签发（对齐 clover PartnerAppServiceImpl：JWT + Redis 上下文）。
 */
@Service
public class PartnerTokenAppServiceImpl implements IPartnerTokenAppService {

    private static final String GRANT_CLIENT_CREDENTIALS = "client_credentials";
    private static final String JWT_TYP_PARTNER = "partner";
    private static final String MASKED_SECRET = "******";
    private static final String UNKNOWN_PARTNER_ID = "UNKNOWN";

    private final IPartnerDomainService partnerDomainService;
    private final PartnerTokenRedisStore tokenRedisStore;
    private final OpenApiProperties properties;
    private final IInvocationDomainService invocationDomainService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PartnerTokenAppServiceImpl(IPartnerDomainService partnerDomainService,
                                      PartnerTokenRedisStore tokenRedisStore,
                                      OpenApiProperties properties,
                                      IInvocationDomainService invocationDomainService) {
        this.partnerDomainService = partnerDomainService;
        this.tokenRedisStore = tokenRedisStore;
        this.properties = properties;
        this.invocationDomainService = invocationDomainService;
    }

    @Override
    public ApiResponse<PartnerTokenIssueResponse> issueToken(PartnerTokenIssueRequest request) {
        PartnerCredentialDO credential = findCredentialQuietly(request);
        InvocationContext ctx = buildTokenInvocationContext(resolveAuditPartnerId(credential));
        ctx.setRequestBodyJson(maskTokenRequest(request));
        invocationDomainService.start(ctx);
        try {
            ApiResponse<PartnerTokenIssueResponse> response = issueTokenInternal(request, credential);
            invocationDomainService.finish(ctx, sanitizeTokenResponse(response));
            return response;
        } catch (OpenApiException ex) {
            invocationDomainService.finish(ctx, ApiResponse.of(ex.getCode(), ex.getMessage(), null));
            throw ex;
        } catch (Exception ex) {
            invocationDomainService.finish(ctx, OpenApiConstants.CODE_ENGINE_FAILED, "服务内部错误");
            throw ex;
        }
    }

    private ApiResponse<PartnerTokenIssueResponse> issueTokenInternal(PartnerTokenIssueRequest request,
                                                                      PartnerCredentialDO credential) {
        if (request == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "请求体不能为空");
        }
        if (!GRANT_CLIENT_CREDENTIALS.equalsIgnoreCase(request.getGrantType())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "grantType 必须为 client_credentials");
        }
        if (credential == null || !PartnerConstants.STATUS_ACTIVE.equals(credential.getStatus())) {
            return ApiResponse.of(OpenApiConstants.CODE_AUTH_FAILED, "Invalid client credentials", null);
        }
        if (!passwordEncoder.matches(request.getClientSecret(), credential.getClientSecretHash())) {
            return ApiResponse.of(OpenApiConstants.CODE_AUTH_FAILED, "Invalid client credentials", null);
        }

        PartnerDO partner = partnerDomainService.requireByPartnerId(credential.getPartnerId());
        if (!PartnerConstants.STATUS_ACTIVE.equals(partner.getStatus())) {
            return ApiResponse.of(OpenApiConstants.CODE_AUTH_FAILED, "Partner 未激活或不存在", null);
        }

        List<String> capabilities = partnerDomainService.listCapabilities(partner.getPartnerId());
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

    private PartnerCredentialDO findCredentialQuietly(PartnerTokenIssueRequest request) {
        if (request == null || !StringUtils.hasText(request.getClientId())) {
            return null;
        }
        return partnerDomainService.findCredentialByClientId(request.getClientId());
    }

    private String resolveAuditPartnerId(PartnerCredentialDO credential) {
        if (credential != null && StringUtils.hasText(credential.getPartnerId())) {
            return credential.getPartnerId();
        }
        return UNKNOWN_PARTNER_ID;
    }

    private InvocationContext buildTokenInvocationContext(String partnerId) {
        HttpServletRequest request = currentRequest();
        String httpMethod = request != null ? request.getMethod() : "POST";
        String requestPath = request != null ? request.getRequestURI() : "/oauth/token";
        String clientIp = resolveClientIp(request);
        InvocationContext ctx = new InvocationContext(partnerId, PartnerContext.getRequestId(),
                OpenApiOperations.ISSUE_PARTNER_TOKEN, httpMethod, requestPath, clientIp);
        ctx.setResourceType(OpenApiOperations.RESOURCE_TYPE_AUTH);
        ctx.setResourceId(partnerId);
        return ctx;
    }

    private static HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }

    private String maskTokenRequest(PartnerTokenIssueRequest request) {
        JSONObject body = new JSONObject(true);
        if (request != null) {
            body.put("grantType", request.getGrantType());
            body.put("clientId", request.getClientId());
            body.put("clientSecret", MASKED_SECRET);
        }
        return body.toJSONString();
    }

    private ApiResponse<Map<String, Object>> sanitizeTokenResponse(ApiResponse<PartnerTokenIssueResponse> response) {
        Map<String, Object> data = null;
        if (response != null && response.getData() != null) {
            data = new LinkedHashMap<>();
            data.put("tokenType", response.getData().getTokenType());
            data.put("expiresIn", response.getData().getExpiresIn());
            data.put("partnerId", response.getData().getPartnerId());
        }
        return ApiResponse.of(response != null ? response.getCode() : OpenApiConstants.CODE_ENGINE_FAILED,
                response != null ? response.getMessage() : "服务内部错误", data);
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
