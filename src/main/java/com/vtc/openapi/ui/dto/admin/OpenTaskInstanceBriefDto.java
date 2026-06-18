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
}
