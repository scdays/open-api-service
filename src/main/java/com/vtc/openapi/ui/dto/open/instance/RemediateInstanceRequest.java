package com.vtc.openapi.ui.dto.open.instance;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 修复实例请求。
 */
@Data
@ApiModel(description = "修复实例请求")
public class RemediateInstanceRequest {

    @ApiModelProperty(value = "修复方式")
    private String srcMethod;
    @ApiModelProperty(value = "修复说明")
    private String remedDesc;
    @ApiModelProperty(value = "修复链接")
    private String fixLnk;
}