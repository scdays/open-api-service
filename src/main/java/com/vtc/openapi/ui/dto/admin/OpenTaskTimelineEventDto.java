package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("OpenTaskTimelineEventDto")
public class OpenTaskTimelineEventDto {

    @ApiModelProperty("事件描述")
    private String label;

    @ApiModelProperty("时间")
    private String at;

    @ApiModelProperty("状态 done/active/pending")
    private String state;
}
