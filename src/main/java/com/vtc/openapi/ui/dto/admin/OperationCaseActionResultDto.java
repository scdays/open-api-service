package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("运营案件运营操作结果")
public class OperationCaseActionResultDto {

    @ApiModelProperty("是否成功")
    private boolean success;

    @ApiModelProperty("说明")
    private String message;

    @ApiModelProperty("案件 ID")
    private String caseId;

    @ApiModelProperty("操作类型")
    private String actionType;
}
