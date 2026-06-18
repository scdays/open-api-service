package com.vtc.openapi.domain.open;

public final class OpenApiConstants {

    public static final String HEADER_PARTNER_ID = "X-Partner-Id";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";
    /** Webhook 验签头：HMAC-SHA256 签名（文档 §6） */
    public static final String HEADER_WEBHOOK_SIGNATURE = "X-Webhook-Signature";
    /** Webhook 验签头：Unix 秒时间戳（文档 §6） */
    public static final String HEADER_WEBHOOK_TIMESTAMP = "X-Webhook-Timestamp";
    public static final String API_PREFIX = "/api/open/v1";

    public static final int CODE_OK = 0;
    public static final int CODE_PARAM_ERROR = 40001;
    public static final int CODE_STATE_INVALID = 40002;
    public static final int CODE_CROSS_PARTNER = 40003;
    public static final int CODE_TYPE_INVALID = 40004;
    public static final int CODE_DUPLICATE_OP = 40005;
    public static final int CODE_IDEMPOTENT_CONFLICT = 40901;
    public static final int CODE_AUTH_FAILED = 40101;

    public static final String REDIS_KEY_TOKEN_PREFIX = "partner:token:";

    public static final String REDIS_KEY_CREDENTIAL_PREFIX = "partner:credential:";

    /** 实例写操作幂等缓存 Redis key 前缀：partner:idem:{partnerId}:{idempotencyKey} */
    public static final String REDIS_KEY_IDEMPOTENCY_PREFIX = "partner:idem:";

    public static final String SUBJECT_TYPE_PARTNER = "PARTNER";

    public static final int CODE_ENGINE_FAILED = 50001;

    public static final int CODE_WEBHOOK_FAILED = 50002;

    /** 创建任务受理状态 */
    public static final String TASK_ACCEPT_ACCEPTED = "ACCEPTED";
    public static final String TASK_ACCEPT_QUEUED = "QUEUED";
    public static final String TASK_ACCEPT_REJECTED = "REJECTED";
    /** VTC/引擎下发失败，可重试（Partner 创建接口仍返回 ACCEPTED） */
    public static final String TASK_DISPATCH_FAILED = "DISPATCH_FAILED";

    private OpenApiConstants() {
    }
}
