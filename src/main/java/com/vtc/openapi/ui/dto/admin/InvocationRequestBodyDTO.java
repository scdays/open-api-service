package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("InvocationRequestBodyDTO")
public class InvocationRequestBodyDTO {

    @ApiModelProperty("调用记录 ID")
    private String invocationId;

    @ApiModelProperty("requestId")
    private String requestId;

    @ApiModelProperty("请求体字节数（UTF-8 字符串长度近似）")
    private Long byteSize;

    @ApiModelProperty("是否来自持久化完整请求体（false 表示摘要重建）")
    private Boolean stored;

    @ApiModelProperty("格式化后的 Request Body JSON")
    private String bodyFormatted;
}
