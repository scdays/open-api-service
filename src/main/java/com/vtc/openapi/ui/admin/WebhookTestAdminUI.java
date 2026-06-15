package com.vtc.openapi.ui.admin;

import com.vtc.openapi.infra.config.OpenApiProperties;
import com.vtc.openapi.infra.dev.WebhookTestInbox;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.WebhookTestInboxPageDto;
import com.vtc.openapi.ui.dto.admin.WebhookTestReceiptDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * View or clear the in-memory webhook test inbox (internal admin API).
 */
@RestController
@RequestMapping("/internal/admin/webhook-test")
@ConditionalOnProperty(prefix = "open-api.webhook", name = "test-receiver-enabled", havingValue = "true", matchIfMissing = true)
@Api(tags = "Webhook test inbox")
public class WebhookTestAdminUI {

    private final WebhookTestInbox inbox;
    private final OpenApiProperties properties;

    public WebhookTestAdminUI(WebhookTestInbox inbox, OpenApiProperties properties) {
        this.inbox = inbox;
        this.properties = properties;
    }

    @ApiOperation("List webhook test inbox entries")
    @GetMapping("/inbox")
    public ApiResponse<WebhookTestInboxPageDto> listInbox(
            @RequestParam(value = "eventType", required = false) String eventType,
            @RequestParam(value = "size", defaultValue = "20") int size,
            HttpServletRequest request) {
        List<WebhookTestReceiptDto> items = inbox.list(eventType, size).stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        WebhookTestInboxPageDto page = new WebhookTestInboxPageDto();
        page.setTotal(inbox.count(eventType));
        page.setItems(items);
        page.setCallbackUrl(buildCallbackUrl(request));
        return ApiResponse.ok(page);
    }

    @ApiOperation("Clear webhook test inbox")
    @DeleteMapping("/inbox")
    public ApiResponse<Void> clearInbox() {
        inbox.clear();
        return ApiResponse.ok(null);
    }

    private WebhookTestReceiptDto toDto(WebhookTestInbox.WebhookTestReceipt receipt) {
        WebhookTestReceiptDto dto = new WebhookTestReceiptDto();
        BeanUtils.copyProperties(receipt, dto);
        return dto;
    }

    private String buildCallbackUrl(HttpServletRequest request) {
        String path = properties.getWebhook().getTestReceiverPath();
        if (path == null || path.isEmpty()) {
            path = "/internal/dev/webhook/receive";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        if (defaultPort) {
            return scheme + "://" + host + path;
        }
        return scheme + "://" + host + ":" + port + path;
    }
}
