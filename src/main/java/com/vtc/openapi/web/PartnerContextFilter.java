package com.vtc.openapi.web;

import com.alibaba.fastjson.JSON;
import com.vtc.openapi.common.OpenApiConstants;
import com.vtc.openapi.common.PartnerContext;
import com.vtc.openapi.web.dto.ApiResponse;
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

/**
 * 仅从请求头注入 Partner 上下文；禁止信任 body/query 中的 partnerId。
 * <p>对 {@code /api/open/v1/**}（除 OAuth 别名）无 {@code X-Partner-Id} 时直接返回 {@code 40001}。
 * 生产流量须经 partner-gateway 注入该头；内网直连仅用于联调，见联调手册。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class PartnerContextFilter extends OncePerRequestFilter {

    private static final String OAUTH_ALIAS_PATH = OpenApiConstants.API_PREFIX + "/oauth/token";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || !path.startsWith(OpenApiConstants.API_PREFIX)) {
            return true;
        }
        return OAUTH_ALIAS_PATH.equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String partnerId = request.getHeader(OpenApiConstants.HEADER_PARTNER_ID);
        if (!StringUtils.hasText(partnerId)) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            ApiResponse<Void> body = ApiResponse.of(OpenApiConstants.CODE_PARAM_ERROR,
                    "缺少请求头 X-Partner-Id", null);
            response.getWriter().write(JSON.toJSONString(body));
            return;
        }
        PartnerContext.set(partnerId.trim(), PartnerContext.getRequestId());
        try {
            chain.doFilter(request, response);
        } finally {
            PartnerContext.clear();
        }
    }
}
