package com.vtc.openapi.infra.webhook;

import com.vtc.openapi.domain.partner.model.entity.PartnerWebhookConfigDO;
import com.vtc.openapi.domain.partner.repository.IPartnerRepository;
import com.vtc.openapi.infra.config.OpenApiProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resolves Partner webhook callback URL; falls back to built-in test receiver when enabled.
 */
@Component
public class WebhookCallbackUrlResolver {

    private final IPartnerRepository partnerRepository;
    private final OpenApiProperties properties;

    public WebhookCallbackUrlResolver(IPartnerRepository partnerRepository, OpenApiProperties properties) {
        this.partnerRepository = partnerRepository;
        this.properties = properties;
    }

    public String resolveForPartner(String partnerId) {
        if (!StringUtils.hasText(partnerId)) {
            return defaultTestReceiverUrl();
        }
        PartnerWebhookConfigDO config = partnerRepository.findWebhookConfig(partnerId.trim());
        if (config != null && StringUtils.hasText(config.getCallbackUrl())) {
            return config.getCallbackUrl().trim();
        }
        return defaultTestReceiverUrl();
    }

    public String defaultTestReceiverUrl() {
        if (!properties.getWebhook().isTestReceiverEnabled()) {
            return null;
        }
        return joinBaseAndPath(
                properties.getWebhook().getTestReceiverBaseUrl(),
                properties.getWebhook().getTestReceiverPath());
    }

    public String resolveForCreate(String requestedCallbackUrl) {
        if (StringUtils.hasText(requestedCallbackUrl)) {
            return requestedCallbackUrl.trim();
        }
        return defaultTestReceiverUrl();
    }

    private static String joinBaseAndPath(String base, String path) {
        if (!StringUtils.hasText(base) || !StringUtils.hasText(path)) {
            return null;
        }
        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBase + normalizedPath;
    }
}
