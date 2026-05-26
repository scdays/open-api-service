package com.vtc.openapi.domain.open;

public final class OpenApiConstants {

    public static final String HEADER_PARTNER_ID = "X-Partner-Id";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String API_PREFIX = "/api/open/v1";

    public static final int CODE_OK = 0;
    public static final int CODE_PARAM_ERROR = 40001;
    public static final int CODE_CROSS_PARTNER = 40003;
    public static final int CODE_IDEMPOTENT_CONFLICT = 40901;
    public static final int CODE_AUTH_FAILED = 40101;

    public static final String REDIS_KEY_TOKEN_PREFIX = "partner:token:";

    public static final String REDIS_KEY_CREDENTIAL_PREFIX = "partner:credential:";

    public static final String SUBJECT_TYPE_PARTNER = "PARTNER";

    public static final int CODE_ENGINE_FAILED = 50001;

    public static final int CODE_WEBHOOK_FAILED = 50002;

    private OpenApiConstants() {
    }
}
