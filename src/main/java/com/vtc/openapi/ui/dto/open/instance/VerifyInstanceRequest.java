package com.vtc.openapi.ui.dto.open.instance;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 验证实例请求（§5.3.1）。
 */
@Data
@ApiModel(description = "验证实例请求")
public class VerifyInstanceRequest {

    @ApiModelProperty(value = "漏洞类型 1/2，默认取自实例")
    private Integer vulnType;
    @ApiModelProperty(value = "VALID→2；FALSE_POSITIVE→3", required = true,
            allowableValues = "VALID,FALSE_POSITIVE")
    private String verifyResult;
    @ApiModelProperty(value = "verifyResult=VALID 时必填，如 1021、1026")
    private Integer srcMethod;
    @ApiModelProperty(value = "状态变更时间（Unix 秒字符串）")
    private String transferTime;
    @ApiModelProperty(value = "操作人（审计）", required = true)
    private String operator;
    @ApiModelProperty(value = "备注")
    private String remark;
}
