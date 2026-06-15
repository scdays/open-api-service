package com.vtc.openapi.infra.filter;

import com.alibaba.fastjson.JSON;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.partner.context.PartnerContext;
import com.vtc.openapi.infra.config.OpenApiProperties;
import com.vtc.openapi.ui.dto.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 内网 Partner 管理 API 鉴权：请求头 {@code X-Internal-Admin-Key} 须与配置一致。
 * <p>仅保护 {@code /internal/admin/**}；公网 Partner 流量走 partner-gateway，不经过本过滤器。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class InternalAdminAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_ADMIN_KEY = "X-Internal-Admin-Key";

    private final OpenApiProperties properties;

    public InternalAdminAuthFilter(OpenApiProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/internal/admin/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String configured = properties.getAdmin().getApiKey();
        String provided = request.getHeader(HEADER_ADMIN_KEY);
        if (!StringUtils.hasText(configured) || !Objects.equals(configured, provided)) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            ApiResponse<Void> body = ApiResponse.of(OpenApiConstants.CODE_AUTH_FAILED,
                    "内网管理 API 鉴权失败", null);
            response.getWriter().write(JSON.toJSONString(body));
            return;
        }
        chain.doFilter(request, response);
    }
}
