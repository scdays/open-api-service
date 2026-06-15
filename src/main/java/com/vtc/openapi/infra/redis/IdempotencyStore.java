package com.vtc.openapi.infra.redis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.infra.config.OpenApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 实例写操作幂等缓存（文档 §4.2）。
 * 以 (partnerId, Idempotency-Key) 缓存首次响应，TTL 默认 24h。
 */
@Component
public class IdempotencyStore {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyStore.class);

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    private final OpenApiProperties properties;

    public IdempotencyStore(OpenApiProperties properties) {
        this.properties = properties;
    }

    public CachedResponse find(String partnerId, String idempotencyKey) {
        if (stringRedisTemplate == null
                || !StringUtils.hasText(partnerId)
                || !StringUtils.hasText(idempotencyKey)) {
            return null;
        }
        try {
            String json = stringRedisTemplate.opsForValue().get(redisKey(partnerId, idempotencyKey));
            if (!StringUtils.hasText(json)) {
                return null;
            }
            JSONObject obj = JSON.parseObject(json);
            CachedResponse resp = new CachedResponse();
            resp.setBodyHash(obj.getString("bodyHash"));
            resp.setCode(obj.getIntValue("code"));
            resp.setMessage(obj.getString("message"));
            resp.setData(obj.getString("data"));
            return resp;
        } catch (Exception ex) {
            log.warn("IdempotencyStore.find 失败: partnerId={} key={}", partnerId, idempotencyKey, ex);
            return null;
        }
    }

    public void save(String partnerId, String idempotencyKey, String bodyHash,
                     int code, String message, String dataJson) {
        if (stringRedisTemplate == null
                || !StringUtils.hasText(partnerId)
                || !StringUtils.hasText(idempotencyKey)) {
            return;
        }
        try {
            JSONObject obj = new JSONObject();
            obj.put("bodyHash", bodyHash);
            obj.put("code", code);
            obj.put("message", message);
            obj.put("data", dataJson);

            long ttl = properties.getIdempotency().getTtlSeconds();
            stringRedisTemplate.opsForValue().set(
                    redisKey(partnerId, idempotencyKey),
                    obj.toJSONString(),
                    ttl, TimeUnit.SECONDS);
        } catch (Exception ex) {
            log.warn("IdempotencyStore.save 失败: partnerId={} key={}", partnerId, idempotencyKey, ex);
        }
    }

    private static String redisKey(String partnerId, String idempotencyKey) {
        return OpenApiConstants.REDIS_KEY_IDEMPOTENCY_PREFIX
                + partnerId + ":" + idempotencyKey;
    }

    public static class CachedResponse {
        private String bodyHash;
        private int code;
        private String message;
        private String data;

        public String getBodyHash() { return bodyHash; }
        public void setBodyHash(String bodyHash) { this.bodyHash = bodyHash; }
        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
    }
}
