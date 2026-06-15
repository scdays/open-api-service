package com.vtc.openapi.ui.dto.open.instance;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Data;

/**
 * 实例搜索请求。
 */
@Data
@ApiModel(description = "实例搜索请求")
public class InstanceSearchRequest {

    @ApiModelProperty(value = "任务 ID")
    private String taskId;
    @ApiModelProperty(value = "外部任务 ID")
    private String extTaskId;
    @ApiModelProperty(value = "实例状态列表，如 [0,1,2]")
    private List<Integer> vulInfoStatList;
    @ApiModelProperty(value = "漏洞等级列表，如 [\"high\",\"critical\"]")
    private List<String> vulLevelList;
    @ApiModelProperty(value = "IP/域名")
    private String vulNetAddr;
    @ApiModelProperty(value = "资产名称")
    private String assetName;
    @ApiModelProperty(value = "漏洞名称")
    private String vulName;
    @ApiModelProperty(value = "组织漏洞 ID")
    private String orgVulId;
    @ApiModelProperty(value = "漏洞 ID")
    private String vulId;
    @ApiModelProperty(value = "是否可达（true/false）")
    private Boolean isAccess;
    @ApiModelProperty(value = "单元类型")
    private String unitType;
    @ApiModelProperty(value = "页码，默认 1", required = true)
    private Integer page = 1;
    @ApiModelProperty(value = "每页条数，默认 20", required = true)
    private Integer size = 20;
}