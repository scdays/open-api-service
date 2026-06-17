package com.vtc.openapi.ui.dto.open.instance;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 修复核验请求（§5.5.1）。
 */
@Data
@ApiModel(description = "修复核验请求")
public class VerifyFixInstanceRequest {

    @ApiModelProperty(value = "状态变更时间（Unix 秒字符串）")
    private String transferTime;
    @ApiModelProperty(value = "备注")
    private String remark;
}
