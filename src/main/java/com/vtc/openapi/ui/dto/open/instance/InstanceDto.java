package com.vtc.openapi.ui.dto.open.instance;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 实例列表项（§5.2.1 data.items[]）。
 */
@Data
@ApiModel(description = "实例摘要")
public class InstanceDto {

    @ApiModelProperty(value = "系统漏洞实例 ID", required = true)
    private String vulInfoID;
    @ApiModelProperty(value = "产品漏洞编号")
    private String vulID;
    @ApiModelProperty(value = "实例状态", required = true)
    private Integer vulInfoStat;
    @ApiModelProperty(value = "未修复原因")
    private Integer lvRsn;
    @ApiModelProperty(value = "漏洞名称", required = true)
    private String vulName;
    @ApiModelProperty(value = "危害等级")
    private Integer vulLevel;
    @ApiModelProperty(value = "原始编号（如 CVE）")
    private String orgVulId;
    @ApiModelProperty(value = "网络地址")
    private String vulNetAddr;
    @ApiModelProperty(value = "端口")
    private Integer vulPort;
    @ApiModelProperty(value = "服务")
    private String vulSvc;
    @ApiModelProperty(value = "0=内网，1=互联网")
    private Integer isAccess;
    @ApiModelProperty(value = "状态变更时间", required = true)
    private String transferTime;
    @ApiModelProperty(value = "引擎处置 ID")
    private String vulnDisposalId;
    @ApiModelProperty(value = "Partner 扩展引用")
    private String extVulnRef;
}
