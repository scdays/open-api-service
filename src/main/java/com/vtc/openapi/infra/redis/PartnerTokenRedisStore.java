package com.vtc.openapi.infra.redis;

import com.alibaba.fastjson.JSON;
import com.vtc.openapi.common.OpenApiConstants;
import com.vtc.openapi.ui.dto.auth.PartnerTokenIntrospectResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Component
public class PartnerTokenRedisStore {

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    public void saveToken(String accessToken, PartnerTokenIntrospectResponse context, long ttlSeconds) {
        if (stringRedisTemplate == null) {
            throw new IllegalStateException("Redis 未配置，无法写入 partner:token");
        }
        String key = tokenKey(accessToken);
        String json = JSON.toJSONString(context);
        stringRedisTemplate.opsForValue().set(key, json, ttlSeconds, TimeUnit.SECONDS);
    }

    public PartnerTokenIntrospectResponse getByToken(String accessToken) {
        if (stringRedisTemplate == null || !StringUtils.hasText(accessToken)) {
            return null;
        }
        String json = stringRedisTemplate.opsForValue().get(tokenKey(accessToken));
        if (!StringUtils.hasText(json)) {
            return null;
        }
        return JSON.parseObject(json, PartnerTokenIntrospectResponse.class);
    }

    public void saveCredentialMeta(String clientId, String partnerId, String status) {
        if (stringRedisTemplate == null) {
            return;
        }
        String key = OpenApiConstants.REDIS_KEY_CREDENTIAL_PREFIX + clientId;
        CredentialMeta meta = new CredentialMeta();
        meta.setPartnerId(partnerId);
        meta.setStatus(status);
        stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(meta));
    }

    public static String tokenKey(String accessToken) {
        return OpenApiConstants.REDIS_KEY_TOKEN_PREFIX + sha256Hex(accessToken);
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    public static class CredentialMeta {
        private String partnerId;
        private String status;

        public String getPartnerId() {
            return partnerId;
        }

        public void setPartnerId(String partnerId) {
            this.partnerId = partnerId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
