package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("TASK_SCAN 案件载荷")
public class OperationCaseTaskScanPayloadDto {

    @ApiModelProperty("平台任务 ID")
    private String taskId;

    @ApiModelProperty("OPEN 编排工作台投影（等同 OpenTaskWorkspaceDto）")
    private OpenTaskWorkspaceDto taskWorkspace;
}
