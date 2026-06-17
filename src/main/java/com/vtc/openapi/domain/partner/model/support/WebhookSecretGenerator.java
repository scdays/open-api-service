package com.vtc.openapi.domain.partner.model.support;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Webhook HMAC-SHA256 验签密钥生成器。
 */
public final class WebhookSecretGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private WebhookSecretGenerator() {
    }

    public static String generate() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return "whsec_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
