package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("OpenTaskInstanceBriefDto")
public class OpenTaskInstanceBriefDto {

    @ApiModelProperty("漏洞实例 ID")
    private String vulInfoId;

    @ApiModelProperty("漏洞状态 1/2/3/...")
    private Integer vulInfoStat;

    @ApiModelProperty("资产地址")
    private String address;

    @ApiModelProperty("端口")
    private String port;

    @ApiModelProperty("漏洞名称")
    private String vulnName;

    @ApiModelProperty("漏洞等级")
    private String level;

    @ApiModelProperty("组织漏洞编号（CVE/CNNVD 等）")
    private String orgVulId;

    @ApiModelProperty("关联子任务 subId（任务当时跃迁日志）")
    private String subId;

    @ApiModelProperty("扫描阶段 1=排查 2=验证")
    private Integer scanPhase;

    @ApiModelProperty("核验前状态（修复核验场景）")
    private Integer previousStat;

    @ApiModelProperty("核验后状态（修复核验场景，有跃迁 log 时）")
    private Integer resultStat;
}
