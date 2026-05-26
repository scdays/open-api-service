package com.vtc.openapi.ui.dto;

import com.vtc.openapi.domain.partner.context.PartnerContext;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "ApiResponse", description = "开放平台统一 API 响应")
public class ApiResponse<T> {

    @ApiModelProperty(value = "业务状态码，0 表示成功")
    private int code;

    @ApiModelProperty(value = "提示信息")
    private String message;

    @ApiModelProperty(value = "业务数据")
    private T data;

    @ApiModelProperty(value = "请求追踪 ID")
    private String requestId;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = 0;
        r.message = "ok";
        r.data = data;
        r.requestId = PartnerContext.getRequestId();
        return r;
    }

    public static <T> ApiResponse<T> of(int code, String message, T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = code;
        r.message = message;
        r.data = data;
        r.requestId = PartnerContext.getRequestId();
        return r;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
