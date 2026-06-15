package com.vtc.openapi.ui.dto.open.instance;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 核验修复请求。
 */
@Data
@ApiModel(description = "核验修复请求")
public class VerifyFixInstanceRequest {

    @ApiModelProperty(value = "核验结果: FIX_CONFIRMED=确认已修复, FIX_FAILED=确认未修复", required = true,
            allowableValues = "FIX_CONFIRMED,FIX_FAILED")
    private String verifyResult;
}