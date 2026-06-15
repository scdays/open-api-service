package com.vtc.openapi.infra.adapter;

import com.vtc.openapi.domain.open.OpenApiConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Webhook 出站 HTTP 投递适配器，POST JSON 到目标 URL。
 * 携带 HMAC-SHA256 验签头（文档 §6）：
 * <ul>
 *   <li>X-Webhook-Signature: body 的 HMAC-SHA256（密钥为 webhookSecret）</li>
 *   <li>X-Webhook-Timestamp: Unix 秒时间戳</li>
 * </ul>
 */
@Component
public class WebhookDeliveryAdapter {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryAdapter.class);

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final RestTemplate restTemplate;

    public WebhookDeliveryAdapter() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * POST JSON 到目标 URL，返回 HTTP 状态码。
     * 若提供 webhookSecret，则附加验签头。
     *
     * @param url           目标回调 URL
     * @param jsonPayload   请求体 JSON
     * @param webhookSecret HMAC 密钥（可为空，空时不验签）
     */
    public int post(String url, String jsonPayload, String webhookSecret) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            if (StringUtils.hasText(webhookSecret)) {
                String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
                String signature = hmacSha256Hex(webhookSecret, jsonPayload);
                headers.set(OpenApiConstants.HEADER_WEBHOOK_SIGNATURE, signature);
                headers.set(OpenApiConstants.HEADER_WEBHOOK_TIMESTAMP, timestamp);
            }

            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return response.getStatusCodeValue();
        } catch (Exception ex) {
            log.warn("Webhook POST 失败: url={}", url, ex);
            throw new RuntimeException("Webhook 投递 HTTP 错误", ex);
        }
    }

    /** 计算 HMAC-SHA256 并返回 hex 小写 */
    public static String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("HMAC-SHA256 计算失败", ex);
        }
    }
}