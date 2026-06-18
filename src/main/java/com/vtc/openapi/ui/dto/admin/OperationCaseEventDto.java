package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel("OperationCaseEventDto")
public class OperationCaseEventDto {

    @ApiModelProperty("事件 ID")
    private Long id;

    @ApiModelProperty("事件类型")
    private String eventType;

    @ApiModelProperty("载荷 JSON")
    private String eventPayloadJson;

    @ApiModelProperty("创建时间")
    private Date createdAt;
}
