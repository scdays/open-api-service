package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("待修复核验实例（运营）")
public class VerifyFixPendingInstanceDto {

    @ApiModelProperty("实例 ID")
    private String vulInfoId;

    @ApiModelProperty("系统漏洞 ID")
    private String vulId;

    @ApiModelProperty("漏洞名称")
    private String vulName;

    @ApiModelProperty("网络地址")
    private String vulNetAddr;

    @ApiModelProperty("端口")
    private Integer vulPort;

    @ApiModelProperty("当前状态")
    private Integer vulInfoStat;

    @ApiModelProperty("关联任务")
    private String taskId;

    @ApiModelProperty("修复核验 jobId")
    private String verifyFixJobId;

    @ApiModelProperty("运营案件")
    private String caseId;

    @ApiModelProperty("选举来源 sub_id")
    private String sourceSubId;

    @ApiModelProperty("选举扫描器")
    private String scannerType;

    @ApiModelProperty("复扫 sub_id")
    private String rescanSubId;

    @ApiModelProperty("复扫子任务状态")
    private String rescanSubStatus;

    @ApiModelProperty("复扫进度")
    private Integer rescanProgress;

    @ApiModelProperty("报告下载路径")
    private String reportDownloadPath;

    @ApiModelProperty("条目状态")
    private String itemStatus;
}
