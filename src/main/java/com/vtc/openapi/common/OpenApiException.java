package com.vtc.openapi.common;

public class OpenApiException extends RuntimeException {

    private final int code;

    public OpenApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
