package com.vtc.openapi.ui.dto.open.instance;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 实例详情 DTO。
 */
@Data
@ApiModel(description = "实例详情")
public class InstanceDetailDto {

    @ApiModelProperty(value = "实例唯一 ID")
    private String vulInfoID;
    @ApiModelProperty(value = "漏洞 ID")
    private String vulID;
    @ApiModelProperty(value = "实例状态")
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
    @ApiModelProperty(value = "地址类型")
    private String vulAddrType;
    @ApiModelProperty(value = "资产 ID")
    private String assetID;
    @ApiModelProperty(value = "资产名称")
    private String assetName;
    @ApiModelProperty(value = "组件 CPE")
    private String vulInstCpe;
    @ApiModelProperty(value = "组件厂商")
    private String vulInstVendor;
    @ApiModelProperty(value = "组件分类")
    private String vulInstClass;
    @ApiModelProperty(value = "组件名称")
    private String vulInstName;
    @ApiModelProperty(value = "组件版本")
    private String vulInstVer;
    @ApiModelProperty(value = "修复说明")
    private String remedDesc;
    @ApiModelProperty(value = "修复链接")
    private String fixLnk;
    @ApiModelProperty(value = "修复时间")
    private String remedTime;
    @ApiModelProperty(value = "修复方式")
    private String srcMethod;
    @ApiModelProperty(value = "传输协议")
    private String vulTransProto;
    @ApiModelProperty(value = "外部漏洞引用")
    private String extVulnRef;
}