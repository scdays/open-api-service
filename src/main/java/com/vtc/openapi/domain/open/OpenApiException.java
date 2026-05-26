package com.vtc.openapi.domain.open;

public class OpenApiException extends RuntimeException {

    private final int code;
    private final Object data;

    public OpenApiException(int code, String message) {
        this(code, message, null);
    }

    public OpenApiException(int code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public Object getData() {
        return data;
    }
}
