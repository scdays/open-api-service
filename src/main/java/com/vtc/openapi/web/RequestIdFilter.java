package com.vtc.openapi.web;

import com.vtc.openapi.common.OpenApiConstants;
import com.vtc.openapi.common.PartnerContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * 全链路 requestId：优先透传 X-Request-Id，否则生成。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = request.getHeader(OpenApiConstants.HEADER_REQUEST_ID);
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = "req-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        String existingPartner = request.getHeader(OpenApiConstants.HEADER_PARTNER_ID);
        try {
            PartnerContext.set(existingPartner, requestId);
            response.setHeader(OpenApiConstants.HEADER_REQUEST_ID, requestId);
            chain.doFilter(request, response);
        } finally {
            PartnerContext.clear();
        }
    }
}
