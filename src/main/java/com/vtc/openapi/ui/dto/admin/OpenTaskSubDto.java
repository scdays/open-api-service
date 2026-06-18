package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@ApiModel("OpenTaskSubDto")
public class OpenTaskSubDto {

    @ApiModelProperty("子任务 ID")
    private String subId;

    @ApiModelProperty("所属 open_task")
    private String taskId;

    @ApiModelProperty("扫描阶段 1=排查 2=验证 3=修复核验")
    private Integer scanPhase;

    @ApiModelProperty("扫描器类型 1=绿盟 7=Nessus")
    private String scannerType;

    @ApiModelProperty("扫描器显示名")
    private String scannerLabel;

    @ApiModelProperty("task-center 任务类型 vuln/port/alive")
    private String centerTaskType;

    @ApiModelProperty("计划 ID")
    private String centerPlanId;

    @ApiModelProperty("计划实例 surveyId")
    private String surveyId;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("进度")
    private Integer progress;

    @ApiModelProperty("错误信息")
    private String errorMessage;

    @ApiModelProperty("创建时间")
    private String createdAt;

    @ApiModelProperty("更新时间")
    private String updatedAt;

    @ApiModelProperty("VTC 报告 FTP 路径")
    private String reportDownloadPath;

    @ApiModelProperty("原始报告归档 fileKey（文件服务）")
    private String reportFileField;

    @ApiModelProperty("关联 verifyFixJobId（phase=3）")
    private String verifyFixJobId;
}
