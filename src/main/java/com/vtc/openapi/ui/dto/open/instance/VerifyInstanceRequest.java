package com.vtc.openapi.ui.dto.open.instance;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 验证实例请求。
 */
@Data
@ApiModel(description = "验证实例请求")
public class VerifyInstanceRequest {

    @ApiModelProperty(value = "验证结果: VALID=有效, FALSE_POSITIVE=误报", required = true,
            allowableValues = "VALID,FALSE_POSITIVE")
    private String verifyResult;
}