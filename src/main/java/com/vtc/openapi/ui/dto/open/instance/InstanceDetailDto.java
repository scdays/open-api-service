package com.vtc.openapi.ui.dto.open.instance;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import lombok.Data;

/**
 * 实例详情（§5.2.2）。
 */
@Data
@ApiModel(description = "实例详情")
public class InstanceDetailDto {

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
    @ApiModelProperty(value = "地址类型：1=IPv4, 2=IPv6, 3=HTTP, 4=N/A, 5=其他")
    private Integer vulAddrType;
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
    @ApiModelProperty(value = "修复方案说明")
    private String remedDesc;
    @ApiModelProperty(value = "补丁链接")
    private String fixLnk;
    @ApiModelProperty(value = "防护/阻断设备")
    private String defDev;
    @ApiModelProperty(value = "修复耗时，如 3日")
    private String remedTime;
    @ApiModelProperty(value = "处置方式")
    private Integer srcMethod;
    @ApiModelProperty(value = "传输协议")
    private String vulTransProto;
    @ApiModelProperty(value = "企业内部备案说明")
    private String archiveReason;
    @ApiModelProperty(value = "省侧扩展 JSON")
    private Map<String, Object> provincialFields;
}
