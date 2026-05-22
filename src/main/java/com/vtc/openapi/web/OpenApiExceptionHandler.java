package com.vtc.openapi.web;

import com.vtc.openapi.common.OpenApiConstants;
import com.vtc.openapi.common.OpenApiException;
import com.vtc.openapi.web.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.vtc.openapi")
public class OpenApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(OpenApiExceptionHandler.class);

    @ExceptionHandler(OpenApiException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleOpenApi(OpenApiException ex) {
        return ApiResponse.of(ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleValidation(Exception ex) {
        String msg = ex.getMessage();
        if (ex instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException manv = (MethodArgumentNotValidException) ex;
            if (manv.getBindingResult().getFieldError() != null) {
                msg = manv.getBindingResult().getFieldError().getDefaultMessage();
            }
        }
        return ApiResponse.of(OpenApiConstants.CODE_PARAM_ERROR, msg != null ? msg : "参数错误", null);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleOther(Exception ex) {
        log.error("open-api unhandled error", ex);
        return ApiResponse.of(OpenApiConstants.CODE_ENGINE_FAILED, "服务内部错误", null);
    }
}
