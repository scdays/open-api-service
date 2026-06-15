package com.vtc.openapi.infra.interceptor;

import com.alibaba.fastjson.JSON;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiOperations;
import com.vtc.openapi.domain.partner.context.PartnerContext;
import com.vtc.openapi.infra.filter.CachedBodyHttpServletRequest;
import com.vtc.openapi.infra.redis.IdempotencyStore;
import com.vtc.openapi.infra.redis.PartnerTokenRedisStore;
import com.vtc.openapi.ui.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 实例写操作幂等拦截器（文档 §4.2）。
 * 仅对 6 个实例写接口且携带 Idempotency-Key 时生效：
 * <ul>
 *   <li>命中缓存 + body 一致 → 直接返回首次响应</li>
 *   <li>命中缓存 + body 不一致 → 40901</li>
 *   <li>未命中 → 放行，执行后由 InvocationPipeline 落缓存</li>
 * </ul>
 */
@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyInterceptor.class);

    /** 需要幂等控制的 operationId 集合 */
    private static final Set<String> IDEMPOTENT_OPERATIONS = new HashSet<>();
    static {
        IDEMPOTENT_OPERATIONS.add(OpenApiOperations.VERIFY_INSTANCE);
        IDEMPOTENT_OPERATIONS.add(OpenApiOperations.REMEDIATE_INSTANCE);
        IDEMPOTENT_OPERATIONS.add(OpenApiOperations.VERIFY_FIX_INSTANCE);
        IDEMPOTENT_OPERATIONS.add(OpenApiOperations.VERIFY_INSTANCE_BATCH);
        IDEMPOTENT_OPERATIONS.add(OpenApiOperations.REMEDIATE_INSTANCE_BATCH);
        IDEMPOTENT_OPERATIONS.add(OpenApiOperations.VERIFY_FIX_INSTANCE_BATCH);
    }

    /** 请求属性：本次 Idempotency-Key（preHandle 设置，Pipeline 读取后清除） */
    public static final String ATTR_IDEMPOTENT_KEY = "openapi.idempotency.key";
    public static final String ATTR_IDEMPOTENT_BODY_HASH = "openapi.idempotency.bodyHash";

    private final IdempotencyStore idempotencyStore;

    public IdempotencyInterceptor(IdempotencyStore idempotencyStore) {
        this.idempotencyStore = idempotencyStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String idempotencyKey = request.getHeader(OpenApiConstants.HEADER_IDEMPOTENCY_KEY);
        if (!StringUtils.hasText(idempotencyKey)) {
            return true;
        }

        if (!isInstanceWritePath(request.getRequestURI())) {
            return true;
        }

        String partnerId = PartnerContext.requirePartnerId();
        String bodyHash = computeBodyHash(request);

        IdempotencyStore.CachedResponse cached = idempotencyStore.find(partnerId, idempotencyKey);
        if (cached == null) {
            request.setAttribute(ATTR_IDEMPOTENT_KEY, idempotencyKey);
            request.setAttribute(ATTR_IDEMPOTENT_BODY_HASH, bodyHash);
            return true;
        }

        if (!Objects.equals(bodyHash, cached.getBodyHash())) {
            writeConflict(response, "Idempotency-Key 已存在但请求体不一致");
            return false;
        }

        log.debug("幂等命中: partnerId={} key={} → 重放首次响应", partnerId, idempotencyKey);
        writeReplay(response, cached);
        return false;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
    }

    /** 判断请求路径是否为实例写操作 */
    private static boolean isInstanceWritePath(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        return path.contains("/instances/") && (path.endsWith("/verify")
                || path.endsWith("/remediate")
                || path.endsWith("/verify-fix")
                || path.endsWith("/verify:batch")
                || path.endsWith("/remediate:batch")
                || path.endsWith("/verify-fix:batch"));
    }

    /** 计算请求体 SHA-256 摘要；无 body 时以空串摘要 */
    private static String computeBodyHash(HttpServletRequest request) {
        String body = extractCachedBody(request);
        if (!StringUtils.hasText(body)) {
            body = "";
        }
        return PartnerTokenRedisStore.sha256Hex(body);
    }

    /** 从 Filter 包装链中读取 ContentCachingFilter 缓存的 body */
    private static String extractCachedBody(HttpServletRequest request) {
        HttpServletRequest current = request;
        while (current != null) {
            if (current instanceof CachedBodyHttpServletRequest) {
                return ((CachedBodyHttpServletRequest) current).getCachedBodyAsString();
            }
            if (current instanceof HttpServletRequestWrapper) {
                current = (HttpServletRequest) ((HttpServletRequestWrapper) current).getRequest();
            } else {
                break;
            }
        }
        return "";
    }

    private static void writeConflict(HttpServletResponse response, String message) throws java.io.IOException {
        ApiResponse<Void> body = ApiResponse.of(OpenApiConstants.CODE_IDEMPOTENT_CONFLICT, message, null);
        writeJson(response, body);
    }

    private static void writeReplay(HttpServletResponse response,
                                     IdempotencyStore.CachedResponse cached) throws java.io.IOException {
        String dataJson = cached.getData();
        Object data = StringUtils.hasText(dataJson) ? JSON.parse(dataJson) : null;
        ApiResponse<Object> body = new ApiResponse<>();
        body.setCode(cached.getCode());
        body.setMessage(cached.getMessage());
        body.setData(data);
        body.setRequestId(PartnerContext.getRequestId());
        writeJson(response, body);
    }

    private static void writeJson(HttpServletResponse response, Object body) throws java.io.IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JSON.toJSONString(body));
    }
}