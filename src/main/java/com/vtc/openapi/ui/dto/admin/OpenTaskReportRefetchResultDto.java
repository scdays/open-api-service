package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 重新获取/归档原始扫描报告结果。
 */
@ApiModel("OpenTaskReportRefetchResultDto")
@Data
public class OpenTaskReportRefetchResultDto {

    @ApiModelProperty("任务 ID")
    private String taskId;

    @ApiModelProperty("子任务 ID（单子任务重试时返回）")
    private String subId;

    @ApiModelProperty("是否成功（单子任务：本次归档是否成功；全部：是否存在可重试子任务）")
    private Boolean success;

    @ApiModelProperty("说明")
    private String message;

    @ApiModelProperty("归档后状态: WAITING_PATH/PENDING/ARCHIVED/FAILED")
    private String reportArchiveStatus;

    @ApiModelProperty("本次尝试归档的子任务数（全部重试时返回）")
    private Integer attempted;

    @ApiModelProperty("本次归档成功的子任务数（全部重试时返回）")
    private Integer archived;

    @ApiModelProperty("本次归档失败的子任务 subId 列表")
    private List<String> failedSubIds;
}
