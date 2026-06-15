package com.vtc.openapi.ui.dto.open.instance;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Data;

/**
 * 实例批量操作响应。
 */
@Data
@ApiModel(description = "实例批量操作响应")
public class InstanceBatchOperationResponse {

    @ApiModelProperty(value = "成功列表")
    private List<InstanceOperationResponse> success;
    @ApiModelProperty(value = "失败列表")
    private List<InstanceBatchFailedItem> failed;
}