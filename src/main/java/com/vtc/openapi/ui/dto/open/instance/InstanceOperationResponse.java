package com.vtc.openapi.ui.dto.open.instance;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 实例写操作响应。
 */
@Data
@ApiModel(description = "实例操作响应")
public class InstanceOperationResponse {

    @ApiModelProperty(value = "实例唯一 ID")
    private String vulInfoID;
    @ApiModelProperty(value = "操作前状态")
    private Integer previousStatus;
    @ApiModelProperty(value = "操作后状态")
    private Integer currentStatus;
}