package com.vtc.openapi.app.open;

import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.partner.context.PartnerContext;
import com.vtc.openapi.domain.open.service.business.IApiCatalogDomainService;
import com.vtc.openapi.domain.open.model.InvocationContext;
import com.vtc.openapi.domain.open.service.business.IInvocationDomainService;
import com.vtc.openapi.infra.interceptor.IdempotencyInterceptor;
import com.vtc.openapi.infra.redis.IdempotencyStore;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 统一调用管道：登记 invocation → 执行业务 Handler → 完成 invocation → 统一响应包装。
 */
@Component
public class InvocationPipeline {

    private static final Logger log = LoggerFactory.getLogger(InvocationPipeline.class);

    private final IApiCatalogDomainService apiCatalogDomainService;
    private final IInvocationDomainService invocationDomainService;
    private final IdempotencyStore idempotencyStore;

    public InvocationPipeline(IApiCatalogDomainService apiCatalogDomainService,
                              IInvocationDomainService invocationDomainService,
                              IdempotencyStore idempotencyStore) {
        this.apiCatalogDomainService = apiCatalogDomainService;
        this.invocationDomainService = invocationDomainService;
        this.idempotencyStore = idempotencyStore;
    }

    public <T> ApiResponse<T> invoke(String operationId, OpenOperationHandler<T> handler) {
        apiCatalogDomainService.requirePublished(operationId);
        InvocationContext ctx = buildContext(operationId);
        invocationDomainService.start(ctx);

        ApiResponse<T> response = ApiResponse.of(OpenApiConstants.CODE_ENGINE_FAILED, "服务内部错误", null);
        try {
            T data = handler.execute(ctx);
            response = ApiResponse.ok(data);
        } catch (OpenApiException ex) {
            @SuppressWarnings("unchecked")
            T payload = (T) ex.getData();
            response = ApiResponse.of(ex.getCode(), ex.getMessage(), payload);
        } catch (Exception ex) {
            log.error("open-api pipeline error operationId={} requestId={}",
                    operationId, ctx.getRequestId(), ex);
            response = ApiResponse.of(OpenApiConstants.CODE_ENGINE_FAILED, "服务内部错误", null);
        } finally {
            response.setRequestId(ctx.getRequestId());
            invocationDomainService.finish(ctx, response);
            cacheIdempotentResponse(ctx, response);
        }
        return response;
    }

    /**
     * 若请求携带 Idempotency-Key 且未被拦截器命中（首次请求），
     * 将本次响应落缓存，供后续重放（文档 §4.2）。
     */
    private <T> void cacheIdempotentResponse(InvocationContext ctx, ApiResponse<T> response) {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return;
        }
        String idempotencyKey = (String) request.getAttribute(IdempotencyInterceptor.ATTR_IDEMPOTENT_KEY);
        if (!StringUtils.hasText(idempotencyKey)) {
            return;
        }
        String bodyHash = (String) request.getAttribute(IdempotencyInterceptor.ATTR_IDEMPOTENT_BODY_HASH);
        request.removeAttribute(IdempotencyInterceptor.ATTR_IDEMPOTENT_KEY);
        request.removeAttribute(IdempotencyInterceptor.ATTR_IDEMPOTENT_BODY_HASH);

        String dataJson = response.getData() != null ? JSON.toJSONString(response.getData()) : null;
        idempotencyStore.save(ctx.getPartnerId(), idempotencyKey, bodyHash,
                response.getCode(), response.getMessage(), dataJson);
    }

    private InvocationContext buildContext(String operationId) {
        String partnerId = PartnerContext.requirePartnerId();
        String requestId = PartnerContext.getRequestId();
        HttpServletRequest request = currentRequest();
        String httpMethod = request != null ? request.getMethod() : "UNKNOWN";
        String requestPath = request != null ? request.getRequestURI() : "";
        String clientIp = resolveClientIp(request);
        return new InvocationContext(partnerId, requestId, operationId, httpMethod, requestPath, clientIp);
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
}
