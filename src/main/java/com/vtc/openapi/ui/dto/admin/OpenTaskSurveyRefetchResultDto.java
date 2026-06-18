package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("OpenTaskSurveyRefetchResultDto")
@Data
public class OpenTaskSurveyRefetchResultDto {

    @ApiModelProperty("任务 ID")
    private String taskId;

    @ApiModelProperty("子任务 ID")
    private String subId;

    @ApiModelProperty("是否成功")
    private Boolean success;

    @ApiModelProperty("说明")
    private String message;

    @ApiModelProperty("清除的扫描结果行数（该子任务）")
    private Integer clearedScanRows;

    @ApiModelProperty("清除的漏洞实例数（任务级排查 ingest）")
    private Integer clearedInstances;

    @ApiModelProperty("清除的排查阶段跃迁日志数")
    private Integer clearedSurveyLogs;

    @ApiModelProperty("本次落库扫描结果行数")
    private Integer persistedScanRows;

    @ApiModelProperty("重放后任务状态")
    private String taskStatus;
}
