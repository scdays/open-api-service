package com.vtc.openapi.ui.dto.open.instance;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 实例摘要 DTO（列表页用）。
 */
@Data
@ApiModel(description = "实例摘要")
public class InstanceDto {

    @ApiModelProperty(value = "实例唯一 ID")
    private String vulInfoID;
    @ApiModelProperty(value = "漏洞 ID")
    private String vulID;
    @ApiModelProperty(value = "实例状态: 0=潜在预警 1=初始发现 2=已验证有效 3=误报 5=已修复 6=核验已修复 7=核验未修复")
    private Integer vulInfoStat;
    @ApiModelProperty(value = "等级变更原因")
    private String lvRsn;
    @ApiModelProperty(value = "漏洞名称")
    private String vulName;
    @ApiModelProperty(value = "漏洞等级")
    private String vulLevel;
    @ApiModelProperty(value = "组织漏洞 ID")
    private String orgVulId;
    @ApiModelProperty(value = "IP/域名")
    private String vulNetAddr;
    @ApiModelProperty(value = "端口")
    private String vulPort;
    @ApiModelProperty(value = "服务")
    private String vulSvc;
    @ApiModelProperty(value = "是否可达")
    private Boolean isAccess;
    @ApiModelProperty(value = "流转时间")
    private String transferTime;
    @ApiModelProperty(value = "处置 ID")
    private String vulnDisposalId;
    @ApiModelProperty(value = "外部漏洞引用")
    private String extVulnRef;
}