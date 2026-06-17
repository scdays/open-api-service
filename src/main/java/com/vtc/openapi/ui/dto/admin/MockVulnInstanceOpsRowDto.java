package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("运营页漏洞实例行（open_vuln_instance）")
public class MockVulnInstanceOpsRowDto {

    @ApiModelProperty("实例 ID")
    private String vulInfoId;

    @ApiModelProperty("vulInfoStat")
    private Integer vulInfoStat;

    @ApiModelProperty("所属平台 taskId")
    private String taskId;

    @ApiModelProperty("漏洞名称")
    private String vulName;

    @ApiModelProperty("资产地址")
    private String vulNetAddr;

    @ApiModelProperty("端口")
    private Integer vulPort;

    @ApiModelProperty("待处理修复核验作业 ID（如有）")
    private String pendingVerifyFixJobId;
}
