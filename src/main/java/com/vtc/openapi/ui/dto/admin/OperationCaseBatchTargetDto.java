package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("批量案件目标项")
public class OperationCaseBatchTargetDto {

    @ApiModelProperty("vulInfoID")
    private String targetKey;

    @ApiModelProperty("DONE / FAILED / PENDING")
    private String targetStatus;

    @ApiModelProperty("变更前状态")
    private Integer prevStat;

    @ApiModelProperty("变更后状态")
    private Integer resultStat;

    @ApiModelProperty("单项载荷 JSON")
    private String payloadJson;
}
