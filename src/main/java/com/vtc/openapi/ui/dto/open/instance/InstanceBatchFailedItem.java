package com.vtc.openapi.ui.dto.open.instance;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 批量操作失败条目。
 */
@Data
@ApiModel(description = "批量操作失败条目")
public class InstanceBatchFailedItem {

    @ApiModelProperty(value = "实例唯一 ID")
    private String vulInfoID;
    @ApiModelProperty(value = "错误码")
    private String errorCode;
    @ApiModelProperty(value = "错误信息")
    private String errorMessage;
}