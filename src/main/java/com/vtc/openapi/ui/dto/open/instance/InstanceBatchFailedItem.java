package com.vtc.openapi.ui.dto.open.instance;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 批量操作失败条目（§5.3.2 / §5.4.2 / §5.5.2 failed[]）。
 */
@Data
@ApiModel(description = "批量操作失败条目")
public class InstanceBatchFailedItem {

    @ApiModelProperty(value = "实例 ID", required = true)
    private String vulInfoID;
    @ApiModelProperty(value = "业务错误码", required = true)
    private Integer code;
    @ApiModelProperty(value = "错误描述", required = true)
    private String message;
}
