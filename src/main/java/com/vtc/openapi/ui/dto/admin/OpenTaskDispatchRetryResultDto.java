package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("OpenTaskDispatchRetryResultDto")
@Data
public class OpenTaskDispatchRetryResultDto {

    @ApiModelProperty("任务 ID")
    private String taskId;

    @ApiModelProperty("是否至少有一个子任务下发成功")
    private Boolean success;

    @ApiModelProperty("说明")
    private String message;

    @ApiModelProperty("重试后任务状态")
    private String taskStatus;

    @ApiModelProperty("本次重试子任务数")
    private Integer retriedCount;

    @ApiModelProperty("下发成功数")
    private Integer successCount;

    @ApiModelProperty("仍失败数")
    private Integer failedCount;
}
