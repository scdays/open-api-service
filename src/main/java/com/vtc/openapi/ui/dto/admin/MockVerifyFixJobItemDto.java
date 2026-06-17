package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("修复核验任务目标实例")
public class MockVerifyFixJobItemDto {

    @ApiModelProperty("vulInfoID")
    private String vulInfoId;
    private String taskId;
    private Integer previousStat;
    private Integer resultStat;
    private String itemStatus;
}
