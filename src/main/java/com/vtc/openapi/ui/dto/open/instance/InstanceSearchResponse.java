package com.vtc.openapi.ui.dto.open.instance;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Data;

/**
 * 实例搜索响应。
 */
@Data
@ApiModel(description = "实例搜索响应")
public class InstanceSearchResponse {

    @ApiModelProperty(value = "当前页码")
    private Integer page;
    @ApiModelProperty(value = "每页条数")
    private Integer size;
    @ApiModelProperty(value = "总条数")
    private Long total;
    @ApiModelProperty(value = "实例列表")
    private List<InstanceDto> items;
}